package specscript.commands.connections

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.language.CommandHandler
import specscript.language.SpecScriptCommandError
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import specscript.util.Json

object GetAllCredentials : CommandHandler("Get all credentials", "core/connections"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode {

        val targetName = data.asText()
        val credentials = context.getCredentials()
        val target = credentials.targetResources[targetName] ?: throw SpecScriptCommandError(
            "unknown target",
            "Unknown target $targetName",
            Json.newObject("target", targetName)
        )

        return target.toArrayNode()
    }
}