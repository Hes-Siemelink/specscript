package specscript.commands.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import java.util.*

object Base64Encode : CommandHandler("Base64 encode", "core/util"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode {
        return TextNode(Base64.getEncoder().encodeToString(data.asText().toByteArray()))
    }
}