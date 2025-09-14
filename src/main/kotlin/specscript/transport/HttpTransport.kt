package specscript.transport

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult

/**
 * HTTP transport for communication with HTTP-based MCP servers.
 *
 * This transport uses HTTP requests to communicate with MCP servers
 * that expose their functionality over HTTP endpoints.
 */
class HttpTransport(
    private val baseUrl: String,
    private val headers: Map<String, String> = emptyMap()
) : McpClientTransport {

    private var connected = false

    override suspend fun connect(): Boolean {
        // TODO: Implement in Phase 3
        throw UnsupportedOperationException("HTTP transport not implemented yet - planned for Phase 3")
    }

    override suspend fun callTool(request: CallToolRequest): CallToolResult {
        throw UnsupportedOperationException("HTTP transport not implemented yet - planned for Phase 3")
    }

    override suspend fun listTools(): ListToolsResult {
        throw UnsupportedOperationException("HTTP transport not implemented yet - planned for Phase 3")
    }

    override suspend fun close() {
        connected = false
        // TODO: Cleanup HTTP client resources when implemented
    }
}