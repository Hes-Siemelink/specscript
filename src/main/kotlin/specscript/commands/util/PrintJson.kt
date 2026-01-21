package specscript.commands.util

import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.util.toDisplayJson
import tools.jackson.databind.JsonNode

object PrintJson : CommandHandler("Print Json", "core/util"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {

        println(data.toDisplayJson())

        return null
    }
}