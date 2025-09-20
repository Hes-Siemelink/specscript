package specscript.commands.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent

import kotlinx.coroutines.runBlocking
import specscript.commands.mcp.transport.TransportConfig
import specscript.commands.mcp.transport.TransportFactory
import specscript.language.*
import specscript.util.toDomainObject
import specscript.util.toKotlinx

object CallMcpTool : CommandHandler("Call Mcp tool", "ai/mcp"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(CallMcpToolInfo::class)

        return runBlocking {
            callToolWithTransport(info)
        }
    }

    private suspend fun callToolWithTransport(
        info: CallMcpToolInfo,
    ): JsonNode? {
        val transportConfig = TransportConfig.fromJson(info.transport, info.server)
        val transport = TransportFactory.createTransport(transportConfig)

        return try {
            if (!transport.connect()) {
                throw SpecScriptCommandError("Failed to connect to MCP server '${info.server}'")
            }

            val request = CallToolRequest(
                name = info.tool,
                arguments = info.arguments?.toKotlinx() ?: kotlinx.serialization.json.JsonObject(emptyMap())
            )

            val result = transport.callTool(request)
            result.firstTextAsJson()

        } catch (e: SpecScriptCommandError) {
            throw e
        } catch (e: Exception) {
            throw SpecScriptCommandError("Tool '${info.tool}' call failed: ${e.message}")
        } finally {
            transport.close()
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
        is TextContent -> TextNode(first.text)
        else -> TextNode("Tool executed successfully with result of type ${first.type}")
    }
}


data class CallMcpToolInfo(
    val server: String?,
    val tool: String,
    val transport: JsonNode,
    val arguments: ObjectNode? = null
)