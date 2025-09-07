package specscript.commands.connections

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.getTextParameter

object DeleteCredentials : CommandHandler("Delete credentials", "core/connections"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val targetName = data.getTextParameter("target")
        val oldCredentials = data.getTextParameter("name")
        val credentials = context.getCredentials()
        val target = credentials.targetResources[targetName] ?: return null

        target.credentials.removeIf { it["name"]?.textValue() == oldCredentials }
        credentials.save()

        return null
    }
}