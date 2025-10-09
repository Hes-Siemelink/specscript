package specscript.commands.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import specscript.util.Yaml

object ParseYaml : CommandHandler("Parse Yaml", "core/util"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        return Yaml.parseIfPossible(data.asText())
    }
}