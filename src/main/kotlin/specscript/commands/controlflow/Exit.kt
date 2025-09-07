package specscript.commands.controlflow

import com.fasterxml.jackson.databind.JsonNode
import specscript.language.AnyHandler
import specscript.language.Break
import specscript.language.CommandHandler
import specscript.language.ScriptContext

object Exit : CommandHandler("Exit", "core/control-flow"), AnyHandler {
    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {
        throw Break(data)
    }
}