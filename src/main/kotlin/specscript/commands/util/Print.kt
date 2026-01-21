package specscript.commands.util

import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.util.toDisplayYaml
import tools.jackson.databind.JsonNode

object Print : CommandHandler("Print", "core/util"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {

        println(data.toDisplayYaml())

        return null
    }
}