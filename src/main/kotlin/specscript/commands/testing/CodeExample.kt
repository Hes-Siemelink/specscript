package specscript.commands.testing

import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ValueNode

object CodeExample : CommandHandler("Code example", "core/testing"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        ExpectedConsoleOutput.reset(context)

        return null
    }
}