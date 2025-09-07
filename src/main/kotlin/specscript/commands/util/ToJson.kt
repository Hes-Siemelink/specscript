package specscript.commands.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.util.toCompactJson

object ToJson : CommandHandler("Json", "core/util"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {
        return TextNode(data.toCompactJson())
    }
}