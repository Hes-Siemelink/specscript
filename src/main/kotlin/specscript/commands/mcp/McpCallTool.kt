package specscript.commands.mcp

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import specscript.commands.mcp.transport.HttpClient
import specscript.commands.mcp.transport.McpClientWrapper
import specscript.commands.mcp.transport.StdioClient
import specscript.language.*
import specscript.util.Json
import specscript.util.Yaml
import specscript.util.toDomainObject
import specscript.util.toKotlinx
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

object McpCallTool : CommandHandler("Mcp call tool", "ai/mcp"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(CallMcpToolInfo::class)

        return runBlocking {
            callTool(info)
        }
    }

    private suspend fun callTool(
        info: CallMcpToolInfo,
    ): JsonNode? {
        val mcp = createMcpClient(info.server)

        return try {
            mcp.connect()

            val request = CallToolRequest(
                CallToolRequestParams(
                    name = info.tool,
                    arguments = info.input?.toKotlinx() ?: kotlinx.serialization.json.JsonObject(emptyMap())
                )
            )

            val result = mcp.client.callTool(request)
            val firstMessage: JsonNode = result.firstTextAsJson()
            if (result.isError == true) {
                throw SpecScriptCommandError(
                    "Tool '${info.tool}' call failed",
                    type = "MCP Server error",
                    data = firstMessage
                )
            }

            firstMessage

        } catch (e: Exception) {
            throw SpecScriptCommandError("Tool '${info.tool}' call failed: ${e.message}", cause = e)
        } finally {
            mcp.close()
        }
    }


}

fun CallToolResult.firstTextAsJson(): JsonNode {
    if (content.isEmpty()) {
        return StringNode("Tool executed but returned no content")
    }

    // TODO handle lists and other content types
    val first = content.first()
    return when (first) {
        is TextContent -> Yaml.parseIfPossible(first.text)

        else -> StringNode("Tool executed successfully with result of type ${first.type}")
    }
}

fun createMcpClient(
    server: TargetServerInfo,
): McpClientWrapper {
    return when (server.transport) {
        TransportType.STDIO -> {
            StdioClient(server.command!!)
        }

        TransportType.HTTP -> {
            HttpClient(server.url!!, server.headers, server.token)
        }
    }
}


data class CallMcpToolInfo(
    val tool: String,
    val server: TargetServerInfo,
    val input: ObjectNode? = null
)

data class TargetServerInfo(
    val transport: TransportType = TransportType.HTTP,
    val server: String? = null,
    val command: String? = null,
    val url: String?,
    val headers: Map<String, String> = emptyMap(),
    val token: String? = null,
)