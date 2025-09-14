package specscript.transport

import io.modelcontextprotocol.kotlin.sdk.server.Server
import specscript.commands.mcp.McpServer
import specscript.language.ScriptContext

/**
 * Factory for creating MCP client transports based on configuration.
 */
object TransportFactory {

    /**
     * Creates a transport instance based on the provided configuration.
     */
    fun createTransport(
        config: TransportConfig,
        context: ScriptContext
    ): McpClientTransport {
        return when (config) {
            is TransportConfig.Internal -> {
                val server = McpServer.servers[config.serverName]
                    ?: throw IllegalArgumentException("Server '${config.serverName}' is not running. Start it with 'Mcp server' command first.")
                InternalTransport(server, context)
            }
            is TransportConfig.Stdio -> {
                StdioTransport(config.command)
            }
            is TransportConfig.Http -> {
                HttpTransport(config.url, config.headers)
            }
        }
    }
}