package specscript.transport

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult

/**
 * Abstraction for MCP client transport mechanisms.
 *
 * Supports multiple transport types:
 * - Internal: Direct in-process communication (testing/validation)
 * - Stdio: Shell command execution with stdio pipes
 * - HTTP: HTTP-based MCP server communication
 */
interface McpClientTransport {

    /**
     * Establishes connection to the MCP server.
     * @return true if connection successful, false otherwise
     */
    suspend fun connect(): Boolean

    /**
     * Calls a tool on the connected MCP server.
     * @param request The MCP tool call request
     * @return The tool execution result
     */
    suspend fun callTool(request: CallToolRequest): CallToolResult

    /**
     * Lists available tools on the connected MCP server.
     * @return List of available tools
     */
    suspend fun listTools(): ListToolsResult

    /**
     * Closes the connection and releases resources.
     * Must be called to prevent resource leaks.
     */
    suspend fun close()
}