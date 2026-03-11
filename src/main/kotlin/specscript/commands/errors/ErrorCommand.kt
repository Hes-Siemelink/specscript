package specscript.commands.errors

import specscript.language.*
import specscript.util.toDisplayYaml
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

object ErrorCommand : CommandHandler("Error", "core/errors"), ValueHandler, ObjectHandler, ArrayHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode {
        throw SpecScriptCommandError(data.toDisplayYaml())
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {
        val error = data.toDomainObject(ErrorData::class)

        throw SpecScriptCommandError(message = error.message, type = error.type, data = error.data)
    }

    override fun execute(data: ArrayNode, context: ScriptContext): JsonNode {
        // Prevent behavior of 'default list handler' for errors
        throw CommandFormatException("Error does not support lists")
    }
}