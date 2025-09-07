package specscript.commands.connections

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.getTextParameter

object SetDefaultCredentials : CommandHandler("Set default credentials", "core/connections"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val targetName = data.getTextParameter("target")
        val newDefault = data.getTextParameter("name")
        val credentials = context.getCredentials()
        val target = credentials.targetResources[targetName] ?: return null

        target.default = newDefault
        credentials.save()

        return null
    }
}