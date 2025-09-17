package specscript.commands.mcp

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.CommandHandler
import specscript.language.DelayedResolver
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.util.toDomainObject

object McpPrompt : CommandHandler("Mcp prompt", "ai/mcp"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val promptsData = data.toDomainObject(McpPromptsData::class)
        val server = McpServer.getDefaultServer(context)

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