package specscript.commands.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import specscript.commands.mcp.transport.HttpClient
import specscript.commands.mcp.transport.McpClientWrapper
import specscript.commands.mcp.transport.SseClient
import specscript.commands.mcp.transport.StdioClient
import specscript.language.*
import specscript.util.Yaml
import specscript.util.toDomainObject
import specscript.util.toKotlinx

object CallMcpTool : CommandHandler("Call Mcp tool", "ai/mcp"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(CallMcpToolInfo::class)

        return runBlocking {
            callTool(info)
        }
    }

    private suspend fun callTool(
        info: CallMcpToolInfo,
    ): JsonNode? {
        val mcp = createMcpClient(info.transport)

        return try {
            mcp.connect()

            val request = CallToolRequest(
                name = info.tool,
                arguments = info.arguments?.toKotlinx() ?: kotlinx.serialization.json.JsonObject(emptyMap())
            )

            val result = mcp.client.callTool(request) as CallToolResult
            val firstMessage: JsonNode = result.firstTextAsJson()
            if (result.isError!!) {
                throw SpecScriptCommandError("MCP Server error", "Tool '${info.tool}' call failed", data = firstMessage)
            }

            firstMessage

        } catch (e: Exception) {
            e.printStackTrace()
            throw SpecScriptCommandError("Tool '${info.tool}' call failed: ${e.message}")
        } finally {
            mcp.close()
        }
    }


}

fun CallToolResult.firstTextAsJson(): JsonNode {
    if (content.isEmpty()) {
        return TextNode("Tool executed but returned no content")
    }

    // TODO handle lists and other content types
    val first = content.first()
    return when (first) {
        is TextContent -> {
            Yaml.parseIfPossible(first.text)
        }

        else -> TextNode("Tool executed successfully with result of type ${first.type}")
    }
}

fun createMcpClient(
    transport: TransportInfo,
): McpClientWrapper {
    return when (transport.type) {
        "stdio" -> {
            StdioClient(transport.command!!)
        }

        "http" -> {
            HttpClient(transport.url!!, transport.headers, transport.auth_token, transport.type)
        }

        "sse" -> {
            SseClient(transport.url!!, transport.headers, transport.auth_token, transport.type)
        }

        else -> throw SpecScriptCommandError("Unknown transport type: ${transport.type}")
    }
}


data class CallMcpToolInfo(
    val tool: String,
    val transport: TransportInfo,
    val arguments: ObjectNode? = null  // TODO: rename to 'input'
)

data class TransportInfo(
    val type: String,
    val server: String? = null,
    val command: String? = null,
    val url: String?,
    val headers: Map<String, String> = emptyMap(),
    val auth_token: String? = null,  // TODO: rename to token
)