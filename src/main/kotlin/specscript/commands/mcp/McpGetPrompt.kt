package specscript.commands.mcp

import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequest
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import specscript.commands.mcp.transport.McpClientWrapper
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.SpecScriptCommandError
import specscript.util.Yaml
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

object McpGetPrompt : CommandHandler("Mcp get prompt", "mcp/client"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(GetMcpPromptInfo::class)

        val target = resolveMcpTarget(info.session, info.server != null, "Mcp get prompt", context)

        return runBlocking {
            when (target) {
                is McpTarget.Session -> getPrompt(target.session.client, info)

                McpTarget.ConnectPerCall -> getPromptOnce(info)
            }
        }
    }

    private suspend fun getPromptOnce(info: GetMcpPromptInfo): JsonNode? {
        val mcp = createMcpClient(info.server!!)

        return try {
            try {
                mcp.connect()
            } catch (e: Exception) {
                throw SpecScriptCommandError("Prompt '${info.name}' get failed: ${e.message}", cause = e)
            }

            getPrompt(mcp, info)
        } finally {
            mcp.close()
        }
    }

    private suspend fun getPrompt(mcp: McpClientWrapper, info: GetMcpPromptInfo): JsonNode? {
        return try {
            val arguments = info.arguments?.properties()
                ?.associate { (key, value) -> key to value.stringValue() }

            val request = GetPromptRequest(
                GetPromptRequestParams(
                    name = info.name,
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
            throw SpecScriptCommandError("Prompt '${info.name}' get failed: ${e.message}", cause = e)
        }
    }
}

data class GetMcpPromptInfo(
    val name: String,
    val server: TargetServerInfo? = null,
    val session: String? = null,
    val arguments: ObjectNode? = null,
)
