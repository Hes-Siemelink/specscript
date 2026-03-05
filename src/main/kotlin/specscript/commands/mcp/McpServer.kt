package specscript.commands.mcp

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.modelcontextprotocol.kotlin.sdk.server.*
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import specscript.files.FileContext
import specscript.files.SpecScriptFile
import specscript.language.*
import specscript.util.*
import kotlin.concurrent.thread

object McpServer : CommandHandler("Mcp server", "ai/mcp"), ObjectHandler, DelayedResolver {

    init {
        // Avoid NoClassDefFoundError from Ktor's shutdown hook when the JVM exits via Ctrl+C
        System.setProperty("io.ktor.server.engine.ShutdownHook", "false")
    }

    private const val DEFAULT_MCP_SERVER = "mcp.server.default"

    val servers = mutableMapOf<String, Server>()
    private val httpServers = mutableMapOf<String, HttpMcpServer>()

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(McpServerInfo::class)

        // Stop server
        if (info.stop) {
            this.stopServer(info.name)
            clearCurrentServer(context)
            return null
        }

        // TODO Resolve top level properties but not the scripts

        val server = servers.getOrPut(info.name) {
            Server(
                Implementation(
                    name = info.name,
                    version = info.version
                ),
                ServerOptions(
                    capabilities = ServerCapabilities(
                        tools = ServerCapabilities.Tools(listChanged = true),
                        resources = ServerCapabilities.Resources(subscribe = false, listChanged = true),
                        prompts = ServerCapabilities.Prompts(listChanged = true)
                    )
                )
            )
        }

        info.tools.forEach { (toolName, tool) ->
            server.addTool(toolName, tool, context.clone())
        }

        info.resources.forEach { (resourceURI, resource) ->
            server.addResource(resourceURI, resource, context.clone())
        }

        info.prompts.forEach { (promptName, prompt) ->
            server.addPrompt(promptName, prompt, context.clone())
        }

        // Store current server name in session context for Mcp tool command
        setDefaultServer(context, info.name)

        // Start server with appropriate transport
        startServer(info, server)

        return null
    }

    private fun startServer(info: McpServerInfo, server: Server) {
        when (info.transport) {
            TransportType.STDIO -> startStdioServer(info.name, server)
            TransportType.SSE -> startSseServer(info, server)
            TransportType.HTTP -> startStreamableHttpServer(info, server)
        }
    }

    private fun startStdioServer(name: String, server: Server) {
        val transport = StdioServerTransport(
            System.`in`.asSource().buffered(),
            System.out.asSink().buffered()
        )

        thread(start = true, isDaemon = false, name = "MCP Server - $name") {
            runBlocking {
                server.createSession(transport)

                val done = Job()
                server.onClose {
                    done.complete()
                }
                if (servers.contains(name)) {
                    done.join()
                } else {
                    System.err.println("MCP stdio server '$name' stopped before it could start")
                }
            }
        }
    }

    private fun startSseServer(info: McpServerInfo, server: Server) {

        val ktorServer = embeddedServer(Netty, port = info.port) {
            install(SSE)
            routing {
                mcp("mcp") { server }  // Hardcoded to 'mcp' because mcpStreamableHttp() does so
            }
        }

        httpServers[info.name] = ktorServer
        startAndKeepAlive(ktorServer, info.name)

        println("Started MCP ${info.transport} server '${info.name}' on http://localhost:${info.port}/mcp")
    }

    private fun startStreamableHttpServer(info: McpServerInfo, server: Server) {

        val ktorServer = embeddedServer(Netty, port = info.port) {
            install(ContentNegotiation) {
                json(McpJson)
            }
            mcpStreamableHttp { server }
        }

        httpServers[info.name] = ktorServer
        startAndKeepAlive(ktorServer, info.name)

        println("Started MCP ${info.transport} server '${info.name}' on http://localhost:${info.port}/mcp")
    }

    /** Starts the Ktor server synchronously (no race condition) then keeps the JVM alive with a non-daemon thread. */
    private fun startAndKeepAlive(ktorServer: HttpMcpServer, name: String) {
        ktorServer.start(wait = false)
        thread(start = true, isDaemon = false, name = "MCP keep-alive - $name") {
            runBlocking {
                val done = Job()
                servers[name]?.onClose { done.complete() }
                    ?: return@runBlocking
                done.join()
            }
        }
    }

    fun stopServer(name: String) {
        val server = servers.remove(name)
        val httpServer = httpServers.remove(name)

        if (server != null) {
            runBlocking {
                server.close()
            }
        }

        if (httpServer != null) {
            runBlocking {
                httpServer.stop(100, 200)
            }
        }
    }

    fun getDefaultServer(context: ScriptContext): Server {
        val currentServerName = context.session[DEFAULT_MCP_SERVER] as String
        return servers[currentServerName]
            ?: throw IllegalStateException("No MCP server found in current context. An MCP server must be started before defining tools.")
    }

    private fun setDefaultServer(context: ScriptContext, serverName: String) {
        context.session[DEFAULT_MCP_SERVER] = serverName
    }

    private fun clearCurrentServer(context: ScriptContext) {
        context.session.remove(DEFAULT_MCP_SERVER)
    }

    fun Server.addTool(toolName: String, tool: ToolInfo, localContext: ScriptContext) {

        println(" - Tool: $toolName")

        val resolvedSchema = tool.inputSchema ?: deriveInputSchema(tool, localContext)

        addTool(
            toolName,
            tool.description,
            inputSchema = ToolSchema(
                properties = resolvedSchema?.properties?.toKotlinx() ?: EmptyJsonObject,
                required = resolvedSchema?.required ?: emptyList()
            ),
        ) { request ->
            // Set up context for the tool execution
            localContext.variables[INPUT_VARIABLE] = request.arguments?.toJackson() ?: Json.newObject()


            // TODO handle lists
            try {
                // Run script
                val result: JsonNode? = if (tool.script is TextNode) {
                    // Local script file
                    val file = localContext.scriptDir.resolve(tool.script.textValue())
                    SpecScriptFile(file).run(FileContext(file, localContext, localContext.variables))
                } else {
                    // Inline script
                    tool.script.run(localContext)
                }

                // Process result
                val output = result.toDisplayJson()
                CallToolResult(content = listOf(TextContent(output)))
            } catch (e: SpecScriptException) {
                System.err.println("Tool '$toolName' execution error: ${e.message}")
                CallToolResult(content = listOf(TextContent(e.toString())), isError = true)
            }
        }
    }

    private fun deriveInputSchema(tool: ToolInfo, context: ScriptContext): InputSchema? {
        if (tool.script !is TextNode) return null

        val file = context.scriptDir.resolve(tool.script.textValue())
        val scriptFile = SpecScriptFile(file)
        val inputSchemaCommand = scriptFile.script.commands.find { it.name == "Input schema" }

        if (inputSchemaCommand != null) {
            return inputSchemaCommand.data.toDomainObject(InputSchema::class)
        }

        // Fall back to Input parameters
        val inputParamsCommand = scriptFile.script.commands.find { it.name == "Input parameters" }
        if (inputParamsCommand != null) {
            val info = scriptFile.script.info
            val propertiesNode = Json.mapper.valueToTree<ObjectNode>(info.input)
            return InputSchema(properties = propertiesNode)
        }

        return null
    }

    fun Server.addResource(resourceURI: String, resource: ResourceInfo, localContext: ScriptContext) {

        println(" - Resource: $resourceURI")

        addResource(
            uri = resourceURI,
            name = resource.name,
            description = resource.description,
            mimeType = resource.mimeType
        ) { request ->

            val result: JsonNode? = if (resource.script is TextNode) {
                // Local script file
                val file = localContext.scriptDir.resolve(resource.script.textValue())
                SpecScriptFile(file).run(localContext)
            } else {
                // Inline script
                resource.script.run(localContext)
            }

            ReadResourceResult(
                contents = listOf(
                    TextResourceContents(result.toDisplayJson(), request.uri, resource.mimeType)
                )
            )
        }
    }

    fun Server.addPrompt(promptName: String, prompt: PromptInfo, localContext: ScriptContext) {

        println(" - Prompt: $promptName")

        addPrompt(
            name = promptName,
            description = prompt.description,
            arguments = prompt.arguments.map { argument ->
                PromptArgument(
                    name = argument.name,
                    description = argument.description,
                    required = argument.required
                )
            }
        ) { request ->
            // Set up context for the prompt execution
            localContext.variables[INPUT_VARIABLE] = Json.newObject(request.arguments ?: emptyMap())

            // Run script
            val result: JsonNode? = if (prompt.script is TextNode) {
                // Local script file
                val file = localContext.scriptDir.resolve(prompt.script.textValue())
                SpecScriptFile(file).run(localContext)
            } else {
                // Inline script
                prompt.script.run(localContext)
            }

            // Process result
            GetPromptResult(
                messages = listOf(
                    PromptMessage(
                        role = Role.User,
                        content = TextContent(result.toDisplayJson())
                    )
                ),
                description = "Description for ${request.name}"
            )
        }
    }
}

private typealias HttpMcpServer = EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

data class McpServerInfo(
    val name: String,
    val version: String = "1.0.0",
    val stop: Boolean = false,
    val transport: TransportType = TransportType.HTTP,
    val port: Int = 8080,

    @JsonAnySetter
    val tools: MutableMap<String, ToolInfo> = mutableMapOf(),

    @JsonAnySetter
    val resources: MutableMap<String, ResourceInfo> = mutableMapOf(),

    @JsonAnySetter
    val prompts: MutableMap<String, PromptInfo> = mutableMapOf()
)

enum class TransportType {
    STDIO,
    HTTP,
    SSE
}

data class ToolInfo(
    val description: String,
    val inputSchema: InputSchema?,
    val script: JsonNode
)

data class InputSchema(
    val type: String = "object",
    val properties: ObjectNode,
    val required: List<String> = emptyList()
)

data class ResourceInfo(
    val name: String,
    val description: String,
    val script: JsonNode,
    val mimeType: String = "text/plain"
)

data class PromptInfo(
    val description: String,
    val arguments: List<PromptArgumentInfo> = emptyList(),
    val script: JsonNode
)

data class PromptArgumentInfo(
    val name: String,
    val description: String,
    val required: Boolean = true,
)
