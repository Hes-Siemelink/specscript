package specscript.commands.mcp

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import specscript.files.CliFile
import specscript.language.*
import specscript.util.*
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlin.concurrent.thread

private typealias HttpMcpServer = EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

object McpServer : CommandHandler("Mcp server", "ai/mcp"), ObjectHandler, DelayedResolver {

    private const val CURRENT_MCP_SERVER_KEY = "currentMcpServer"
    
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
        setCurrentServer(context, info.name)
        
        // Start server with appropriate transport
        startServer(info, server)

        return null
    }

    private fun startServer(info: McpServerInfo, server: Server) {
        when (info.transport) {
            TransportType.STDIO -> startStdioServer(info.name, server)
            TransportType.HTTP -> startHttpServer(info, server)
        }
    }

    private fun startStdioServer(name: String, server: Server) {
        val transport = StdioServerTransport(
            System.`in`.asInput(),
            System.out.asSink().buffered()
        )

        thread(start = true, isDaemon = false, name = "MCP Server - $name") {
            System.err.println("[${Thread.currentThread().name}] Starting stdio server ")
            runBlocking {
                server.connect(transport)

                val done = Job()
                server.onClose {
                    done.complete()
                }
                if (servers.contains(name)) {
                    done.join()
                    System.err.println("[${Thread.currentThread().name}] Stopping stdio server ")
                } else {
                    System.err.println("[${Thread.currentThread().name}] Server stopped before it could start")
                }
            }
        }
    }

    private fun startHttpServer(info: McpServerInfo, server: Server) {
        val port = info.port ?: 8080
        val path = info.path ?: "/"
        
        thread(start = true, isDaemon = false, name = "MCP HTTP Server - ${info.name}") {
            System.err.println("[${Thread.currentThread().name}] Starting HTTP server on port $port at path $path")
            
            val ktorServer = embeddedServer(Netty, port = port) {
                routing {
                    route(path) {
                        mcp { server }
                    }
                }
            }
            
            // Store HTTP server reference for shutdown
            httpServers[info.name] = ktorServer
            
            runBlocking {
                try {
                    ktorServer.start(wait = true)
                    System.err.println("[${Thread.currentThread().name}] HTTP server stopped")
                } catch (e: Exception) {
                    System.err.println("[${Thread.currentThread().name}] HTTP server error: ${e.message}")
                } finally {
                    httpServers.remove(info.name)
                }
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
                httpServer.stop(1000, 2000)
            }
        }
    }

    fun getCurrentServer(context: ScriptContext): Server {
        val currentServerName = context.session[CURRENT_MCP_SERVER_KEY] as String
        return servers[currentServerName] ?:
        throw IllegalStateException("No MCP server found in current context. An MCP server must be started before defining tools.")
    }

    private fun setCurrentServer(context: ScriptContext, serverName: String) {
        context.session[CURRENT_MCP_SERVER_KEY] = serverName
    }

    private fun clearCurrentServer(context: ScriptContext) {
        context.session.remove(CURRENT_MCP_SERVER_KEY)
    }

    fun Server.addTool(toolName: String, tool: ToolInfo, localContext: ScriptContext) {

        // TODO add support for required fields
        addTool(
            toolName,
            tool.description,
            Tool.Input(
                properties = tool.inputSchema.toKotlinx()
            ),
        ) { request ->
            // Set up context for the tool execution
            localContext.variables[INPUT_VARIABLE] = request.arguments.toJackson()

            // Run script
            val result: JsonNode? = if (tool.script is TextNode) {
                // Local script file
                val file = localContext.scriptDir.resolve(tool.script.textValue())
                CliFile(file).run(localContext)
            } else {
                // Inline script
                tool.script.run(localContext)
            }

            // Process result
            // TODO handle lists
            // TODO handle errors
            val output = result.toDisplayYaml()
            CallToolResult(content = listOf(TextContent(output)))
        }
    }

    fun Server.addResource(resourceURI: String, resource: ResourceInfo, localContext: ScriptContext) {
        addResource(
            uri = resourceURI,
            name = resource.name,
            description = resource.description,
            mimeType = resource.mimeType
        ) { request ->

            val result: JsonNode? = if (resource.script is TextNode) {
                // Local script file
                val file = localContext.scriptDir.resolve(resource.script.textValue())
                CliFile(file).run(localContext)
            } else {
                // Inline script
                resource.script.run(localContext)
            }

            ReadResourceResult(
                contents = listOf(
                    TextResourceContents(result.toDisplayYaml(), request.uri, resource.mimeType)
                )
            )
        }
    }

    fun Server.addPrompt(promptName: String, prompt: PromptInfo, localContext: ScriptContext) {
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
                CliFile(file).run(localContext)
            } else {
                // Inline script
                prompt.script.run(localContext)
            }

            // Process result
            GetPromptResult(
                "Description for ${request.name}",
                messages = listOf(
                    PromptMessage(
                        role = Role.user,
                        content = TextContent(result.toDisplayYaml())
                    )
                )
            )
        }
    }
}

data class McpServerInfo(
    val name: String,
    val version: String,
    val stop: Boolean = false,
    val transport: TransportType = TransportType.STDIO,
    val port: Int? = null,
    val path: String? = null,

    @JsonAnySetter
    val tools: MutableMap<String, ToolInfo> = mutableMapOf(),

    @JsonAnySetter
    val resources: MutableMap<String, ResourceInfo> = mutableMapOf(),

    @JsonAnySetter
    val prompts: MutableMap<String, PromptInfo> = mutableMapOf()
)

enum class TransportType {
    STDIO,
    HTTP
}

data class ToolInfo(
    val description: String,
    val inputSchema: ObjectNode,
    val script: JsonNode
)

data class ResourceInfo(
    val name: String,
    val description: String,
    val script: JsonNode,
    val mimeType: String = "text/plain"
)

data class PromptInfo(
    val name: String,
    val description: String,
    val arguments: List<PromptArgumentInfo>,
    val script: JsonNode
)

data class PromptArgumentInfo(
    val name: String,
    val description: String,
    val required: Boolean = true,
)
