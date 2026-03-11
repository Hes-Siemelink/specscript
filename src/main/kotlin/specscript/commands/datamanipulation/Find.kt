package specscript.commands.datamanipulation

import specscript.language.*
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

object Find : CommandHandler("Find", "core/data-manipulation"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val path = data.getTextParameter("path")
        val source = data.getParameter("in")

        return source.at(toJsonPointer(path))
    }
}