package specscript.commands.mcp.transport

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.client.Client
import specscript.language.SpecScriptCommandError

/**
 * Abstraction for MCP client transport mechanisms.
 *
 * Supports multiple transport types:
 * - Internal: Direct in-process communication (testing/validation)
 * - Stdio: Shell command execution with stdio pipes
 * - HTTP: HTTP-based MCP server communication
 */
interface McpClient {

    val client: Client

    /**
     * Creates connection to the MCP server.
     */
    suspend fun connect()

    suspend fun close()
}

suspend fun McpClient.callTool(request: CallToolRequest): CallToolResult {

    return try {
        client.callTool(request) as CallToolResult
    } catch (e: Exception) {
        // Re-throw SpecScriptCommandError to preserve error handling behavior
        if (e is SpecScriptCommandError) {
            throw e
        }
        throw Exception("HTTP tool call failed: ${e.message}", e)
    }
}

suspend fun McpClient.listTools(): ListToolsResult {

    return try {
        client.listTools()
    } catch (_: IllegalStateException) {
        // Server does not support tools
        ListToolsResult(tools = emptyList(), nextCursor = null)
    }
}
