package specscript.commands.mcp.transport

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import specscript.language.SpecScriptImplementationException

/**
 * Stdio transport for communication with MCP servers via shell commands.
 *
 * This transport executes shell commands and communicates with the spawned
 * processes using standard input/output pipes following the MCP protocol.
 */
class StdioClient(
    private val command: String
) : McpClient {

    private var process: Process? = null
    override val client: Client = Client(clientInfo = Implementation("specscript-client", "1.0.0"))

    override suspend fun connect() {
        return try {
            // Execute shell command - let shell handle parsing and execution
            println("DEBUG: Starting process with command: $command")
            process = ProcessBuilder("sh", "-c", command).start()

            // Create MCP client with stdio transport
            val transport = StdioClientTransport(
                input = process!!.inputStream.asSource().buffered(),
                output = process!!.outputStream.asSink().buffered()
            )

            client.connect(transport)
            println("DEBUG: Successfully connected to MCP server via stdio")
        } catch (e: Exception) {
            throw SpecScriptImplementationException("Failed to start MCP process: $command", cause = e)
        }
    }

    override suspend fun close() {
        client.close()
        process?.destroy()
    }

}