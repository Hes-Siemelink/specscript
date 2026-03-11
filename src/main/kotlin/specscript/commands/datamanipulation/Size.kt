package specscript.commands.datamanipulation

import specscript.language.*
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.*

object Size : CommandHandler("Size", "core/data-manipulation"), ValueHandler, ArrayHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        when (data) {
            is NumericNode -> {
                return data
            }

            is BooleanNode -> {
                return if (data.booleanValue()) IntNode(1) else IntNode(0)
            }

            is StringNode -> {
                return IntNode(data.stringValue().length)
            }
        }

        throw CommandFormatException("Unsupported type: ${data.javaClass}")
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        return IntNode(data.size())
    }

    override fun execute(data: ArrayNode, context: ScriptContext): JsonNode? {
        return IntNode(data.size())
    }
}