package specscript.commands.testing

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import specscript.language.CommandHandler
import specscript.language.DelayedResolver
import specscript.language.ObjectHandler
import specscript.language.ScriptContext

object Tests : CommandHandler("Tests", "core/testing"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        // No-op in normal execution. Test commands are only executed by the test harness.
        return null
    }
}
