package specscript.transport

import com.fasterxml.jackson.databind.JsonNode

/**
 * Configuration for MCP client transports.
 */
sealed class TransportConfig {

    /**
     * Internal transport configuration for direct in-process communication.
     */
    data class Internal(
        val serverName: String
    ) : TransportConfig()

    /**
     * Stdio transport configuration for shell command execution.
     */
    data class Stdio(
        val command: String
    ) : TransportConfig()

    /**
     * HTTP transport configuration for HTTP-based MCP servers.
     */
    data class Http(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val authToken: String? = null
    ) : TransportConfig()

    companion object {
        /**
         * Parses transport configuration from JSON.
         */
        fun fromJson(transportNode: JsonNode, serverName: String? = null): TransportConfig {
            return when {
                transportNode.isTextual && transportNode.textValue() == "stdio" -> {
                    // Simple stdio transport
                    Stdio("cli ${serverName}.spec.md") // Default command
                }
                transportNode.isObject -> {
                    val type = transportNode.get("type")?.textValue()
                    when (type) {
                        "internal" -> Internal(
                            serverName = serverName ?: throw IllegalArgumentException("serverName required for internal transport")
                        )
                        "stdio" -> {
                            val command = transportNode.get("command")?.textValue()
                                ?: throw IllegalArgumentException("command required for stdio transport")
                            Stdio(command)
                        }
                        "http" -> {
                            val url = transportNode.get("url")?.textValue()
                                ?: throw IllegalArgumentException("url required for http transport")
                            val headers = transportNode.get("headers")?.let { headersNode ->
                                buildMap {
                                    headersNode.fields().forEach { field ->
                                        put(field.key, field.value.asText())
                                    }
                                }
                            } ?: emptyMap()
                            val authToken = transportNode.get("auth_token")?.textValue()
                            Http(url, headers, authToken)
                        }
                        else -> throw IllegalArgumentException("Unknown transport type: $type")
                    }
                }
                else -> throw IllegalArgumentException("Invalid transport configuration")
            }
        }
    }
}