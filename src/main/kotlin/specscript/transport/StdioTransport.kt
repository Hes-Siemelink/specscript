package specscript.transport

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.TextContent

/**
 * Stdio transport for communication with MCP servers via shell commands.
 *
 * This transport executes shell commands and communicates with the spawned
 * processes using standard input/output pipes following the MCP protocol.
 */
class StdioTransport(
    private val command: String
) : McpClientTransport {

    private var connected = false

    override suspend fun connect(): Boolean {
        // TODO: Implement in Phase 2
        throw UnsupportedOperationException("Stdio transport not implemented yet - planned for Phase 2")
    }

    override suspend fun callTool(request: CallToolRequest): CallToolResult {
        throw UnsupportedOperationException("Stdio transport not implemented yet - planned for Phase 2")
    }

    override suspend fun listTools(): ListToolsResult {
        throw UnsupportedOperationException("Stdio transport not implemented yet - planned for Phase 2")
    }

    override suspend fun close() {
        connected = false
        // TODO: Cleanup process resources when implemented
    }
}