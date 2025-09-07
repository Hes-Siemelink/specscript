package specscript.commands.controlflow

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.*

object Do : CommandHandler("Do", "core/control-flow"), ObjectHandler, DelayedResolver {
    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        return data.run(context)
    }
}