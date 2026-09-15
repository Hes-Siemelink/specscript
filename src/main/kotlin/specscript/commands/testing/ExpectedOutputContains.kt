package specscript.commands.testing

import specscript.commands.Contains
import specscript.commands.isFalse
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import specscript.util.Json.newObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

object ExpectedOutputContains : CommandHandler("Expected output contains", "core/testing"), ObjectHandler,
    ValueHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        return contains(data, context)
    }

    override fun execute(
        data: ValueNode,
        context: ScriptContext
    ): JsonNode? {
        return contains(data, context)
    }

    private fun contains(
        data: JsonNode,
        context: ScriptContext
    ): Nothing? {
        val condition = Contains(node = data, container = context.output ?: newObject())

        if (condition.isFalse()) {
            throw AssertionError("Output does not contain expected value.\nExpected part: ${data}\nOutput:${context.output}")
        }

        return null
    }
}