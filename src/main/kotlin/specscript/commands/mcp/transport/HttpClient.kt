package specscript.commands.mcp.transport

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport

/**
 * Transport for communication with HTTP-based MCP server.
 */
class HttpClient(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val authToken: String? = null,
    val type: String = "http"
) : McpClientWrapper {

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
    override val client: Client = Client(clientInfo = Implementation("specscript-client", "1.0.0"))

    override suspend fun connect() {
        println("Connecting to ${type.uppercase()} MCP server at: $url")

        val transport = StreamableHttpClientTransport(
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
        client.connect(transport)
    }

    override suspend fun close() {
        client.close()
        httpClient.close()
    }
}