package specscript.commands.datamanipulation

import specscript.language.ArrayHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.SpecScriptException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.*

object Add : CommandHandler("Add", "core/data-manipulation"), ArrayHandler {

    override fun execute(data: ArrayNode, context: ScriptContext): JsonNode {
        var total: JsonNode = data.first()
        for (item in data.drop(1)) {
            total = add(total, item)
        }
        return total
    }

    fun add(target: JsonNode, item: JsonNode): JsonNode {
        return when (target) {
            is ArrayNode -> addToArray(target, item)
            is ObjectNode -> addToObject(target, item)
            is StringNode -> addToText(target, item)
            is IntNode -> addToInt(target, item)
            else -> throw SpecScriptException("Can't add a ${item.javaClass.simpleName} to a ${target.javaClass.simpleName}")
        }
    }

    private fun addToArray(target: ArrayNode, item: JsonNode): ArrayNode {
        return when (item) {
            is ArrayNode -> target.addAll(item)
            else -> target.add(item)
        }
    }

    private fun addToObject(target: ObjectNode, item: JsonNode): ObjectNode {
        return when (item) {
            is ObjectNode -> target.setAll(item)
            else -> throw SpecScriptException("Can't add a ${item.javaClass.simpleName} to a ${target.javaClass.simpleName}")
        }
    }

    private fun addToText(target: StringNode, item: JsonNode): StringNode {
        return when (item) {
            is ValueNode -> StringNode(target.asString() + item.asString())
            else -> throw SpecScriptException("Can't add a ${item.javaClass.simpleName} to a ${target.javaClass.simpleName}")
        }
    }

    private fun addToInt(target: IntNode, item: JsonNode): IntNode {
        return when (item) {
            is NumericNode -> IntNode(target.asInt() + item.asInt())
            else -> throw SpecScriptException("Can't add a ${item.javaClass.simpleName} to a ${target.javaClass.simpleName}")
        }
    }
}
