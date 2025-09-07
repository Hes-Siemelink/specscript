package specscript.commands.testing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler

object CodeExample : CommandHandler("Code example", "core/testing"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        ExpectedConsoleOutput.reset(context)

        return null
    }
}