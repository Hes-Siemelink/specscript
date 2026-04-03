package specscript.commands.mcp

import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequest
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import specscript.language.*
import specscript.util.Yaml
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

object McpGetPrompt : CommandHandler("Mcp get prompt", "ai/mcp"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(GetMcpPromptInfo::class)

        return runBlocking {
            getPrompt(info)
        }
    }

    private suspend fun getPrompt(info: GetMcpPromptInfo): JsonNode? {
        val mcp = createMcpClient(info.server)

        return try {
            mcp.connect()

            val arguments = info.input?.properties()
                ?.associate { (key, value) -> key to value.stringValue() }

            val request = GetPromptRequest(
                GetPromptRequestParams(
                    name = info.prompt,
                    arguments = arguments
                )
            )

            val result = mcp.client.getPrompt(request)
            val firstMessage = result.messages.firstOrNull()
                ?: return StringNode("Prompt returned no messages")

            when (val content = firstMessage.content) {
                is TextContent -> Yaml.parseIfPossible(content.text)
                else -> StringNode("Prompt returned content of unsupported type")
            }

        } catch (e: Exception) {
            throw SpecScriptCommandError("Prompt '${info.prompt}' get failed: ${e.message}", cause = e)
        } finally {
            mcp.close()
        }
    }
}

data class GetMcpPromptInfo(
    val prompt: String,
    val server: TargetServerInfo,
    val input: ObjectNode? = null,
)
