package specscript.commands.mcp.transport

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport

/**
 * Transport for communication with HTTP or SSE based MCP servers.
 */
class HttpClient(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val authToken: String? = null,
    val type: String = "http"
) : McpClient {

    val httpClient: io.ktor.client.HttpClient = HttpClient() {
        if (authToken != null) {
            install(Auth) {
                bearer {
                    loadTokens { BearerTokens(authToken, "") }
                }
            }
        }
        install(SSE)
    }
    override val client: Client = Client(clientInfo = Implementation("specscript-client", "1.0.0"))

    override suspend fun connect() {
        println("Connecting to ${type.uppercase()} MCP server at: $url")

        val transport = createTransport()
        client.connect(transport)
    }

    private fun createTransport(): AbstractTransport = when (type.lowercase()) {
        "sse" -> {
            SseClientTransport(
                client = httpClient,
                urlString = url,
                requestBuilder = {
                    this@HttpClient.headers.forEach { (key, value) ->
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
                url = url,
                requestBuilder = {
                    this@HttpClient.headers.forEach { (key, value) ->
                        headers {
                            append(key, value)
                        }
                    }
                }
            )
        }
    }

    override suspend fun close() {
        client.close()
        httpClient.close()
    }
}