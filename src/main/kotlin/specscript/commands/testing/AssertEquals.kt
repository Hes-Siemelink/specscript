package specscript.commands.testing

import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.getParameter
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

object AssertEquals : CommandHandler("Assert equals", "core/testing"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val actual = data.getParameter("actual")
        val expected = data.getParameter("expected")

        if (actual != expected) {
            throw AssertionError("Not equal:\n  Expected: $expected\n  Actual:   $actual")
        }

        return null
    }
}