package specscript.commands.mcp

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.*
import specscript.util.*

object McpPrompt : CommandHandler("Mcp prompt", "ai/mcp"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val promptsData = data.toDomainObject(McpPromptsData::class)
        val server = McpServer.getCurrentServer(context)

        // Add each prompt to the running server
        promptsData.prompts.forEach { (promptName, promptInfo) ->
            with(McpServer) {
                server.addPrompt(promptName, promptInfo, context.clone())
            }
        }

        return null
    }
}

class McpPromptsData {
    @JsonAnySetter
    val prompts: MutableMap<String, PromptInfo> = mutableMapOf()
}