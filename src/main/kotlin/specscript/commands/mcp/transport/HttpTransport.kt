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
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import kotlinx.coroutines.runBlocking
import specscript.language.SpecScriptCommandError

/**
 * Transport for communication with HTTP or SSE based MCP servers.
 */
class HttpTransport(
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

    override suspend fun connect() {
        println("Connecting to ${type.uppercase()} MCP server at: $baseUrl")

        val transport = createTransport()
        client.connect(transport)
    }

    private fun createTransport(): AbstractTransport = when (type.lowercase()) {
        "sse" -> {
            SseClientTransport(
                client = httpClient,
                urlString = baseUrl,
                requestBuilder = {
                    this@HttpTransport.headers.forEach { (key, value) ->
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
                    this@HttpTransport.headers.forEach { (key, value) ->
                        headers {
                            append(key, value)
                        }
                    }
                }
            )
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
        try {
            runBlocking {
                client.close()
            }
        } catch (e: Exception) {
            println("Error closing MCP client: $e")
        }

        try {
            httpClient.close()
        } catch (e: Exception) {
            println("Error closing HTTP client: $e")
        }
    }
}