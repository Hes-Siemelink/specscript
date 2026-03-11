package specscript.commands.util

import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode
import tools.jackson.databind.node.ValueNode
import java.util.*

object Base64Encode : CommandHandler("Base64 encode", "core/util"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode {
        return StringNode(Base64.getEncoder().encodeToString(data.asString().toByteArray()))
    }
}