package specscript.commands.controlflow

import specscript.language.*
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

object Do : CommandHandler("Do", "core/control-flow"), ObjectHandler, DelayedResolver {
    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        return data.run(context)
    }
}