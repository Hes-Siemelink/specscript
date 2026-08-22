package specscript.commands.mcp

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import specscript.commands.server.HandlerInfo
import specscript.commands.server.run
import specscript.files.MARKDOWN_SPEC_EXTENSION
import specscript.files.SpecScriptFile
import specscript.files.YAML_SPEC_EXTENSION
import specscript.files.removeSpecScriptExtension
import specscript.language.*
import specscript.util.*
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import specscript.commands.scriptinfo.InputSchema as InputSchemaCommand


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
        val toolScripts: Map<String, ToolInfo> = findScriptsRecursively(context.scriptDir, info.toolFiles)
        info.tools.addTools(toolScripts)

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

    /** Resolves the given list of filenames against [scriptDir] and collects them as tools. Entries that
     * are directories are scanned recursively for spec script files.
     *
     * @param scriptDir the root directory. Filenames are relative to this directory.
     * @param toolFiles the list of filenames to scan. Can be files or directories.
     * @return a map of tool names to ToolInfo objects. The tool name is derived from the filename by stripping the path and extension.
     */
    private fun findScriptsRecursively(
        scriptDir: Path,
        toolFiles: List<String>
    ): Map<String, ToolInfo> {
        val tools = mutableMapOf<String, ToolInfo>()
        for (toolFile in toolFiles) {
            collectScripts(scriptDir.resolve(toolFile), tools)
        }
        return tools
    }

    private fun collectScripts(path: Path, tools: MutableMap<String, ToolInfo>) {
        if (path.isDirectory()) {
            path.listDirectoryEntries().forEach { collectScripts(it, tools) }
        } else if (path.isSpecScriptFile()) {
            val toolName = path.fileName.toString().removeSpecScriptExtension()
            tools[toolName] = ToolInfo(script = StringNode(path.toString()))
        }
    }

    private fun Path.isSpecScriptFile(): Boolean {
        return this.name.endsWith(YAML_SPEC_EXTENSION) || name.endsWith(MARKDOWN_SPEC_EXTENSION)
    }

    private fun MutableMap<String, ToolInfo>.addTools(tools: Map<String, ToolInfo>) {
        for (toolName in tools.keys) {

            if (this.containsKey(toolName)) {
                throw CommandFormatException("Tool name '$toolName' already exists in tools map")
            }

            this[toolName] = tools[toolName] ?: throw CommandFormatException("Tool '$toolName' not found in tools map")
        }
    }

    private fun startServer(info: McpServerInfo, server: Server) {
        when (info.transport) {
            TransportType.STDIO -> startStdioServer(info.name, server)
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

    fun clearCurrentServer(context: ScriptContext) {
        context.session.remove(DEFAULT_MCP_SERVER)
    }

    fun Server.addTool(toolName: String, tool: ToolInfo, localContext: ScriptContext) {

        println(" - Tool: $toolName")

        val needsDerivation = tool.description == null || tool.inputSchema == null
        val derived = if (needsDerivation) deriveFromScript(tool, localContext) else null
        val resolvedSchema = tool.inputSchema ?: derived?.inputSchema
        val resolvedDescription = tool.description ?: derived?.description ?: toolName

        addTool(
            toolName,
            resolvedDescription,
            inputSchema = ToolSchema(
                properties = resolvedSchema?.properties?.toKotlinx() ?: EmptyJsonObject,
                required = resolvedSchema?.required ?: emptyList()
            ),
        ) { request ->
            // Set up context for the tool execution
            localContext.variables[INPUT_VARIABLE] = request.arguments?.toJackson() ?: Json.newObject()

            try {
                val result: JsonNode? = tool.run(localContext)

                // Process result
                val output = result.toDisplayJson()
                CallToolResult(content = listOf(TextContent(output)))
            } catch (e: SpecScriptException) {
                System.err.println("Tool '$toolName' execution error: ${e.message}")
                CallToolResult(content = listOf(TextContent(e.toString())), isError = true)
            }
        }
    }

    private fun deriveFromScript(tool: ToolInfo, context: ScriptContext): DerivedToolMetadata? {
        if (tool.script !is StringNode) return null

        val file = context.scriptDir.resolve(tool.script.stringValue())
        val scriptFile = SpecScriptFile(file)

        return DerivedToolMetadata(
            description = scriptFile.script.info.description,
            inputSchema = deriveInputSchema(scriptFile)
        )
    }

    private fun deriveInputSchema(scriptFile: SpecScriptFile): InputSchema? {
        val inputSchemaCommand = scriptFile.script.commands.find { it.equalsCommand(InputSchemaCommand) }

        if (inputSchemaCommand != null) {
            return inputSchemaCommand.data.toDomainObject(InputSchema::class)
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

            val result: JsonNode? = resource.run(localContext)

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

            val result: JsonNode? = prompt.run(localContext)

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
    val transport: TransportType = TransportType.HTTP,
    val port: Int = 8080,

    val tools: MutableMap<String, ToolInfo> = mutableMapOf(),
    @JsonProperty("tool files")
    val toolFiles: List<String> = listOf(),
    val resources: MutableMap<String, ResourceInfo> = mutableMapOf(),
    val prompts: MutableMap<String, PromptInfo> = mutableMapOf()
)

enum class TransportType {
    STDIO,
    HTTP
}

data class ToolInfo(
    val description: String? = null,
    val inputSchema: InputSchema? = null,
    override val output: JsonNode? = null,
    override val script: JsonNode? = null
) : HandlerInfo

private data class DerivedToolMetadata(
    val description: String?,
    val inputSchema: InputSchema?
)

data class InputSchema(
    val type: String = "object",
    val properties: ObjectNode,
    val required: List<String> = emptyList()
)

data class ResourceInfo(
    val name: String,
    val description: String,
    override val output: JsonNode? = null,
    override val script: JsonNode? = null,
    val mimeType: String = "text/plain"
) : HandlerInfo

data class PromptInfo(
    val description: String,
    val arguments: List<PromptArgumentInfo> = emptyList(),
    override val output: JsonNode? = null,
    override val script: JsonNode? = null
) : HandlerInfo

data class PromptArgumentInfo(
    val name: String,
    val description: String,
    val required: Boolean = true,
)
