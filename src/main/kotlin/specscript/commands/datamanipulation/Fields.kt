package specscript.commands.datamanipulation

import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

object Fields : CommandHandler("Fields", "core/data-manipulation"), ObjectHandler, ValueHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val fields = data.arrayNode()

        for ((key, _) in data.properties()) {
            fields.add(key)
        }

        return fields
    }

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        val output = context.output

        return if (output is ObjectNode) {
            execute(output, context)
        } else {
            null
        }
    }
}