package specscript.commands.util

import specscript.language.CommandFormatException
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ValueNode

object Wait : CommandHandler("Wait", "core/util"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        if (!data.isNumber) {
            throw CommandFormatException("Invalid value for 'Wait' command.")
        }
        val duration = data.doubleValue() * 1000

        Thread.sleep(duration.toLong())

        return null
    }
}