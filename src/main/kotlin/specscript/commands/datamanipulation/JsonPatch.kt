package specscript.commands.datamanipulation

import specscript.language.*
import specscript.util.applyPatch
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode

object JsonPatch : CommandHandler("Json patch", "core/data-manipulation"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {

        val doc = data["doc"]
            ?: context.output
            ?: throw SpecScriptCommandError("Json patch needs 'doc' parameter or non-null output variable.")
        val patch = data.getParameter("patch") as ArrayNode

        return doc.applyPatch(patch)
    }
}