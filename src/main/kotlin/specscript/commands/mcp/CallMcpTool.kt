package specscript.commands.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import specscript.language.*
import specscript.util.*
import specscript.transport.*
import kotlinx.coroutines.runBlocking
import io.modelcontextprotocol.kotlin.sdk.*

object CallMcpTool : CommandHandler("Call mcp tool", "ai/mcp"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val server = data.get("server")?.textValue()
            ?: throw Exception("Missing 'server' property")
        val tool = data.get("tool")?.textValue()
            ?: throw Exception("Missing 'tool' property")
        val transportNode = data.get("transport")
            ?: throw Exception("Missing 'transport' property")
        val argumentsNode = data.get("arguments")

        return runBlocking {
            callToolWithTransport(server, transportNode, tool, argumentsNode, context)
        }
    }

    private suspend fun callToolWithTransport(serverName: String, transportNode: JsonNode, toolName: String, argumentsNode: JsonNode?, context: ScriptContext): JsonNode? {
        // Parse transport configuration
        val transportConfig = TransportConfig.fromJson(transportNode, serverName)

        // Create transport instance
        val transport = TransportFactory.createTransport(transportConfig, context)

        return try {
            // Connect to the server
            if (!transport.connect()) {
                throw Exception("Failed to connect to MCP server")
            }

            // Convert arguments to MCP format
            val arguments = convertArgumentsToMcp(argumentsNode)

            // Create MCP CallToolRequest
            val request = CallToolRequest(
                name = toolName,
                arguments = arguments
            )

            // Call the tool
            val result = transport.callTool(request)

            // Convert result to JsonNode
            convertResultToJsonNode(result)

        } catch (e: Exception) {
            // Re-throw SpecScriptCommandError to preserve error handling behavior
            if (e is specscript.language.SpecScriptCommandError) {
                throw e
            }
            throw Exception("Tool call failed: ${e.message}", e)
        } finally {
            // Always clean up resources
            transport.close()
        }
    }

    /**
     * Converts arguments from JsonNode to MCP format.
     */
    private fun convertArgumentsToMcp(argumentsNode: JsonNode?): kotlinx.serialization.json.JsonObject {
        val arguments = argumentsNode?.let {
            if (it.isObject) {
                buildMap {
                    it.fields().forEach { field ->
                        put(field.key, when {
                            field.value.isTextual -> field.value.asText()
                            field.value.isNumber -> field.value.asDouble()
                            field.value.isBoolean -> field.value.asBoolean()
                            field.value.isNull -> null
                            else -> field.value.toString()
                        })
                    }
                }
            } else {
                emptyMap()
            }
        } ?: emptyMap()

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
    private fun convertResultToJsonNode(result: CallToolResult): JsonNode {
        return when {
            result.content?.isNotEmpty() == true -> {
                val content = result.content.firstOrNull()
                when (content) {
                    is TextContent -> {
                        val text = content.text ?: "Tool executed (empty text content)"

                        // Debug: print the actual text to understand error patterns
                        println("DEBUG: Tool result text: '$text'")

                        TextNode(text)
                    }
                    else -> TextNode("Tool executed successfully")
                }
            }
            else -> TextNode("Tool executed but returned no content")
        }
    }
}