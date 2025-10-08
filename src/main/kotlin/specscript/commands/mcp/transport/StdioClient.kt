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
 */
class StdioClient(
    private val command: String
) : McpClientWrapper {

    override val client: Client = Client(clientInfo = Implementation("specscript-client", "1.0.0"))

    private var process: Process? = null

    override suspend fun connect() {
        return try {
            // Run shell command for server process
            process = ProcessBuilder("sh", "-c", command).start()

            // Connect to process streams
            val transport = StdioClientTransport(
                input = process!!.inputStream.asSource().buffered(),
                output = process!!.outputStream.asSink().buffered()
            )

            client.connect(transport)
        } catch (e: Exception) {
            throw SpecScriptImplementationException("Failed to start MCP process: $command", cause = e)
        }
    }

    override suspend fun close() {
        client.close()
        process?.destroy()
    }
}