package specscript.commands.controlflow

import specscript.language.*
import tools.jackson.databind.JsonNode

object Do : CommandHandler("Do", "core/control-flow"), AnyHandler, DelayedResolver {
    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {
        return data.run(context)
    }
}
