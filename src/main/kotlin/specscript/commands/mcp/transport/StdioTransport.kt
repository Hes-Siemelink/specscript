package specscript.commands.mcp.transport

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import specscript.language.SpecScriptCommandError
import specscript.language.SpecScriptImplementationException
import java.util.concurrent.TimeUnit

/**
 * Stdio transport for communication with MCP servers via shell commands.
 *
 * This transport executes shell commands and communicates with the spawned
 * processes using standard input/output pipes following the MCP protocol.
 */
class StdioTransport(
    private val command: String
) : McpClientTransport {

    private var process: Process? = null
    private var client: Client? = null

    override suspend fun connect() {
        return try {
            // Execute shell command - let shell handle parsing and execution
            println("DEBUG: Starting process with command: $command")
            process = ProcessBuilder("sh", "-c", command).start()

            // Create MCP client with stdio transport
            client = Client(clientInfo = Implementation("specscript-client", "1.0.0"))
            val transport = StdioClientTransport(
                input = process!!.inputStream.asSource().buffered(),
                output = process!!.outputStream.asSink().buffered()
            )

            client!!.connect(transport)
            println("DEBUG: Successfully connected to MCP server via stdio")
        } catch (e: Exception) {
            cleanup()
            throw SpecScriptImplementationException("Failed to start MCP process: $command", cause = e)
        }
    }

    override suspend fun callTool(request: CallToolRequest): CallToolResult {
        val mcpClient = client ?: throw IllegalStateException("Transport not connected. Call connect() first.")

        return try {
            mcpClient.callTool(request) as CallToolResult
        } catch (e: Exception) {
            // Re-throw SpecScriptCommandError to preserve error handling behavior
            if (e is SpecScriptCommandError) {
                throw e
            }
            throw Exception("Stdio tool call failed: ${e.message}", e)
        }
    }

    override suspend fun listTools(): ListToolsResult {
        val mcpClient = client ?: throw IllegalStateException("Transport not connected. Call connect() first.")

        return try {
            mcpClient.listTools() ?: ListToolsResult(tools = emptyList(), nextCursor = null)
        } catch (e: Exception) {
            // Fallback for failed tool listing
            ListToolsResult(tools = emptyList(), nextCursor = null)
        }
    }

    override suspend fun close() {
        cleanup()
    }

    private fun cleanup() {
        println("DEBUG: Cleaning up stdio transport resources")
        try {
            client?.let {
                runBlocking {
                    it.close()
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Error closing MCP client: ${e.message}")
        }

        try {
            process?.let { proc ->
                proc.destroy()
                val terminated = proc.waitFor(5, TimeUnit.SECONDS)
                if (!terminated) {
                    println("DEBUG: Process didn't terminate gracefully, force killing")
                    proc.destroyForcibly()
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Error cleaning up process: ${e.message}")
        } finally {
            process = null
            client = null
        }
    }
}