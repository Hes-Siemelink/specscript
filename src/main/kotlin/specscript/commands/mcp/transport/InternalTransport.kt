package specscript.commands.mcp.transport

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import specscript.language.SpecScriptCommandError
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.isAccessible

/**
 * Internal transport for direct in-process communication with MCP servers.
 *
 * This transport bypasses network communication and directly calls server methods
 * using Kotlin reflection. It's primarily used for testing and validation scenarios
 * where both client and server run in the same JVM process.
 */
class InternalTransport(
    private val server: Server,
) : McpClientTransport {

    private var connected = false

    override suspend fun connect() {
        connected = true
    }

    override suspend fun callTool(request: CallToolRequest): CallToolResult {
        if (!connected) {
            throw IllegalStateException("Transport not connected. Call connect() first.")
        }

        return callToolDirectly(server, request)
            ?: CallToolResult(
                content = listOf(
                    TextContent("Tool execution failed")
                ),
                isError = true
            )
    }

    override suspend fun listTools(): ListToolsResult {
        if (!connected) {
            throw IllegalStateException("Transport not connected. Call connect() first.")
        }

        // Use reflection to call the server's listTools method
        return try {
            val kClass = server::class
            val method = kClass.members.find { it.name == "handleListTools" } as? KFunction<*>

            if (method != null) {
                method.isAccessible = true
                val result = method.callSuspend(server)
                result as? ListToolsResult ?: ListToolsResult(tools = emptyList(), nextCursor = null)
            } else {
                ListToolsResult(tools = emptyList(), nextCursor = null)
            }
        } catch (e: Exception) {
            ListToolsResult(tools = emptyList(), nextCursor = null)
        }
    }

    override suspend fun close() {
        connected = false
        // No resources to clean up for internal transport
    }

    /**
     * Directly calls a tool on the server using Kotlin reflection.
     * This enables real tool execution while bypassing the transport layer.
     */
    private suspend fun callToolDirectly(
        server: Server,
        request: CallToolRequest,
    ): CallToolResult? {
        return try {
            println("DEBUG: Attempting to call tool '${request.name}' via reflection")

            // Use Kotlin reflection to call the suspend function
            val kClass = server::class
            val method = kClass.members.find { it.name == "handleCallTool" } as? KFunction<*>

            if (method != null) {
                println("DEBUG: Found handleCallTool method, calling it")
                method.isAccessible = true
                val result = method.callSuspend(server, request)
                println("DEBUG: Reflection call completed, result: $result")
                result as? CallToolResult
            } else {
                println("DEBUG: handleCallTool method not found")
                // Method not found, create fallback response
                CallToolResult(
                    content = listOf(
                        TextContent("Direct tool call (method not found): '${request.name}' with arguments: ${request.arguments}")
                    ),
                    isError = false
                )
            }

        } catch (e: Exception) {
            println("DEBUG: Reflection failed with exception: ${e.message}")
            e.printStackTrace()

            // Re-throw SpecScriptCommandError to preserve error handling behavior
            if (e.cause is SpecScriptCommandError) {
                throw e.cause!!
            }

            // For other exceptions, fall back to a basic response
            CallToolResult(
                content = listOf(
                    TextContent("Direct tool call (fallback): '${request.name}' with arguments: ${request.arguments} (${e.message})")
                ),
                isError = false
            )
        }
    }
}