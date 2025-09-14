package specscript.commands.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import specscript.language.*
import specscript.transport.TransportConfig
import specscript.transport.TransportFactory
import specscript.util.toDomainObject

object CallMcpTool : CommandHandler("Call mcp tool", "ai/mcp"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(CallMcpToolInfo::class)

        return runBlocking {
            callToolWithTransport(info, context)
        }
    }

    private suspend fun callToolWithTransport(
        info: CallMcpToolInfo,
        context: ScriptContext
    ): JsonNode? {
        val transportConfig = TransportConfig.fromJson(info.transport, info.server)
        val transport = TransportFactory.createTransport(transportConfig, context)

        return try {
            if (!transport.connect()) {
                throw SpecScriptCommandError("Failed to connect to MCP server '${info.server}'")
            }

            val request = CallToolRequest(
                name = info.tool,
                arguments = info.arguments?.toMcp() ?: kotlinx.serialization.json.JsonObject(emptyMap())
            )

            val result = transport.callTool(request)
            result.toJsonNode()

        } catch (e: SpecScriptCommandError) {
            throw e
        } catch (e: Exception) {
            throw SpecScriptCommandError("Tool '${info.tool}' call failed: ${e.message}")
        } finally {
            transport.close()
        }
    }



}

/**
 * Converts JsonNode arguments to MCP-compatible kotlinx.serialization JsonObject.
 */
fun JsonNode.toMcp(): kotlinx.serialization.json.JsonObject {
    val arguments = if (this.isObject) {
        buildMap {
            this@toMcp.fields().forEach { field ->
                put(
                    field.key, when {
                        field.value.isTextual -> field.value.asText()
                        field.value.isNumber -> field.value.asDouble()
                        field.value.isBoolean -> field.value.asBoolean()
                        field.value.isNull -> null
                        else -> field.value.toString()
                    }
                )
            }
        }
    } else {
        emptyMap()
    }

    return kotlinx.serialization.json.JsonObject(
        arguments.mapValues { (_, value) ->
            when (value) {
                is String -> kotlinx.serialization.json.JsonPrimitive(value)
                is Number -> kotlinx.serialization.json.JsonPrimitive(value)
                is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
                null -> kotlinx.serialization.json.JsonNull
                else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
            }
        }
    )
}

/**
 * Converts MCP CallToolResult to JsonNode for SpecScript consumption.
 */
fun CallToolResult.toJsonNode(): JsonNode {
    return when {
        content?.isNotEmpty() == true -> {
            val content = content.firstOrNull()
            when (content) {
                is TextContent -> TextNode(content.text ?: "Tool executed (empty text content)")
                else -> TextNode("Tool executed successfully")
            }
        }
        else -> TextNode("Tool executed but returned no content")
    }
}

data class CallMcpToolInfo(
    val server: String,
    val tool: String,
    val transport: JsonNode,
    val arguments: JsonNode? = null
)