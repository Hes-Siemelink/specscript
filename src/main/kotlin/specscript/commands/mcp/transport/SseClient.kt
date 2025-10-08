package specscript.commands.mcp.transport

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport

/**
 * Transport for communication with SSE-based MCP server.
 */
class SseClient(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val authToken: String? = null,
    val type: String = "http"
) : McpClientWrapper {

    override val client = Client(clientInfo = Implementation("specscript-client", "1.0.0"))

    val httpClient = HttpClient() {
        if (authToken != null) {
            install(Auth) {
                bearer {
                    loadTokens { BearerTokens(authToken, "") }
                }
            }
        }
        install(SSE)
    }

    override suspend fun connect() {
        println("Connecting to ${type.uppercase()} MCP server at: $url")

        val transport = SseClientTransport(
            client = httpClient,
            urlString = url,
            requestBuilder = {
                this@SseClient.headers.forEach { (key, value) ->
                    headers {
                        append(key, value)
                    }
                }
            }
        )
        client.connect(transport)
    }

    override suspend fun close() {
        client.close()
        httpClient.close()
    }
}