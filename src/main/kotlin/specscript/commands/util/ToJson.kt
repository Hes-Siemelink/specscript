package specscript.commands.util

import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.util.toCompactJson
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.TextNode

object ToJson : CommandHandler("Json", "core/util"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {
        return TextNode(data.toCompactJson())
    }
}