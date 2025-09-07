package specscript.commands.testing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.commands.isFalse
import specscript.commands.toCondition
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext

object AssertThat : CommandHandler("Assert that", "core/testing"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val condition = data.toCondition()

        if (condition.isFalse()) {
            throw AssertionError("Condition is false.\n${data}")
        }

        return null
    }
}