package specscript.commands.util

import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.TextNode
import tools.jackson.databind.node.ValueNode
import java.util.*

object Base64Decode : CommandHandler("Base64 decode", "core/util"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        return TextNode(String(Base64.getDecoder().decode(data.asText())))
    }
}