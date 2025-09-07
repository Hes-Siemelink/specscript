package specscript.commands.errors

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.commands.errors.OnError.runErrorHandling
import specscript.language.*

object OnErrorType : CommandHandler("On error type", "core/errors"), ObjectHandler, DelayedResolver, ErrorHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        for ((key, value) in data.fields()) {
            if (key == "any" || key == context.error?.error?.type) {
                runErrorHandling(value, context)
                break
            }
        }

        return null
    }
}