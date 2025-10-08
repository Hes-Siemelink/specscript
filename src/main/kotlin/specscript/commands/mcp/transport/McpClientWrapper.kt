package specscript.commands.mcp.transport

import io.modelcontextprotocol.kotlin.sdk.client.Client

interface McpClientWrapper {

    val client: Client

    suspend fun connect()

    suspend fun close()
}
