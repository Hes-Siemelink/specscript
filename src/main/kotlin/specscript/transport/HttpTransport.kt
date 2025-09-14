package specscript.transport

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*

/**
 * HTTP transport for communication with HTTP-based MCP servers.
 *
 * This transport uses HTTP requests to communicate with MCP servers
 * that expose their functionality over HTTP endpoints.
 */
class HttpTransport(
    private val baseUrl: String,
    private val headers: Map<String, String> = emptyMap(),
    private val authToken: String? = null
) : McpClientTransport {

    private var httpClient: HttpClient? = null
    private var client: Client? = null

    override suspend fun connect(): Boolean {
        return try {
            println("DEBUG: Connecting to HTTP MCP server at: $baseUrl")

            // Create HTTP client with optional authentication
            httpClient = HttpClient(CIO) {
                if (authToken != null) {
                    install(Auth) {
                        bearer {
                            loadTokens { BearerTokens(authToken, "") }
                        }
                    }
                }
            }

            // Create MCP client with HTTP transport
            client = Client(clientInfo = Implementation("specscript-client", "1.0.0"))
            val transport = StreamableHttpClientTransport(
                client = httpClient!!,
                url = baseUrl,
                requestBuilder = {
                    // Add custom headers
                    this@HttpTransport.headers.forEach { (key, value) ->
                        headers {
                            append(key, value)
                        }
                    }
                }
            )

            client!!.connect(transport)
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
        val mcpClient = client ?: throw IllegalStateException("Transport not connected. Call connect() first.")

        return try {
            mcpClient.callTool(request) as CallToolResult
        } catch (e: Exception) {
            // Re-throw SpecScriptCommandError to preserve error handling behavior
            if (e is specscript.language.SpecScriptCommandError) {
                throw e
            }
            throw Exception("HTTP tool call failed: ${e.message}", e)
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
        println("DEBUG: Cleaning up HTTP transport resources")
        try {
            client?.let {
                kotlinx.coroutines.runBlocking {
                    it.close()
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Error closing MCP client: ${e.message}")
        }

        try {
            httpClient?.close()
        } catch (e: Exception) {
            println("DEBUG: Error closing HTTP client: ${e.message}")
        } finally {
            httpClient = null
            client = null
        }
    }
}