package specscript.commands.mcp.transport

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import kotlinx.coroutines.runBlocking
import specscript.language.SpecScriptCommandError

/**
 * HTTP transport for communication with HTTP-based MCP servers.
 *
 * This transport uses HTTP requests to communicate with MCP servers
 * that expose their functionality over HTTP endpoints.
 */
class NetworkTransport(
    private val baseUrl: String,
    private val headers: Map<String, String> = emptyMap(),
    private val authToken: String? = null,
    private val type: String = "http"
) : McpClientTransport {

    private val httpClient: HttpClient = HttpClient() {
        if (authToken != null) {
            install(Auth) {
                bearer {
                    loadTokens { BearerTokens(authToken, "") }
                }
            }
        }
        install(SSE)
    }
    private val client: Client = Client(clientInfo = Implementation("specscript-client", "1.0.0"))

    override suspend fun connect(): Boolean {
        return try {
            println("DEBUG: Connecting to ${type.uppercase()} MCP server at: $baseUrl")

            val transport = when (type.lowercase()) {
                "sse" -> {
                    SseClientTransport(
                        client = httpClient,
                        urlString = baseUrl,
                        requestBuilder = {
                            // Add custom headers
                            this@NetworkTransport.headers.forEach { (key, value) ->
                                headers {
                                    append(key, value)
                                }
                            }
                        }
                    )
                }

                else -> {
                    StreamableHttpClientTransport(
                        client = httpClient,
                        url = baseUrl,
                        requestBuilder = {
                            // Add custom headers
                            this@NetworkTransport.headers.forEach { (key, value) ->
                                headers {
                                    append(key, value)
                                }
                            }
                        })
                }

            }

            client.connect(transport)
            println("DEBUG: Successfully connected to HTTP MCP server")
            true
        } catch (e: Exception) {
            println("DEBUG: Failed to connect to HTTP MCP server: ${e.message}")
            e.printStackTrace()
            cleanup()
            false
        }
    }

    override suspend fun callTool(request: CallToolRequest): CallToolResult {

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

    override suspend fun listTools(): ListToolsResult {

        return try {
            client.listTools()
        } catch (_: IllegalStateException) {
            // Server does not support tools
            ListToolsResult(tools = emptyList(), nextCursor = null)
        }
    }

    override suspend fun close() {
        cleanup()
    }

    private fun cleanup() {
        println("DEBUG: Cleaning up HTTP transport resources")
        try {
            runBlocking {
                client.close()
            }
        } catch (e: Exception) {
            println("DEBUG: Error closing MCP client: ${e.message}")
        }

        try {
            httpClient.close()
        } catch (e: Exception) {
            println("DEBUG: Error closing HTTP client: ${e.message}")
        }
    }
}