package specscript.commands.util

import com.fasterxml.jackson.databind.JsonNode
import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.util.toDisplayJson

object PrintJson : CommandHandler("Print JSON", "core/util"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {

        println(data.toDisplayJson())

        return null
    }
}