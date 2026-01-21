package specscript.commands.util

import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.util.toDisplayYaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.TextNode

object ToText : CommandHandler("Text", "core/util"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {
        return TextNode(data.toDisplayYaml())
    }
}