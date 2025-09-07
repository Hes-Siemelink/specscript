package specscript.commands.datamanipulation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.*

object Find : CommandHandler("Find", "core/data-manipulation"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val path = data.getTextParameter("path")
        val source = data.getParameter("in")

        return source.at(toJsonPointer(path))
    }
}