package specscript.commands.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.language.CommandFormatException
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler

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