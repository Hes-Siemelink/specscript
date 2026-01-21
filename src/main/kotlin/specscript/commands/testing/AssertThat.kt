package specscript.commands.testing

import specscript.commands.isFalse
import specscript.commands.toCondition
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

object AssertThat : CommandHandler("Assert that", "core/testing"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val condition = data.toCondition()

        if (condition.isFalse()) {
            throw AssertionError("Condition is false.\n${data}")
        }

        return null
    }
}