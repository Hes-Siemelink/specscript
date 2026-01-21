package specscript.commands.datamanipulation

import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

object Values : CommandHandler("Values", "core/data-manipulation"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val fields = data.arrayNode()

        for ((_, value) in data.fields()) {
            fields.add(value)
        }

        return fields
    }
}