package specscript.commands.connections

import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.getTextParameter
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

object DeleteCredentials : CommandHandler("Delete credentials", "core/connections"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val targetName = data.getTextParameter("target")
        val oldCredentials = data.getTextParameter("name")
        val credentials = context.getCredentials()
        val target = credentials.targetResources[targetName] ?: return null

        target.credentials.removeIf { it["name"]?.stringValue() == oldCredentials }
        credentials.save()

        return null
    }
}