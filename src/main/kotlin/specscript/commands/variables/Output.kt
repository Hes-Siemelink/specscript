package specscript.commands.variables

import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import tools.jackson.databind.JsonNode

/**
 * Returns the input as output.
 */
object Output : CommandHandler("Output", "core/variables"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode {
        return data
    }
}
