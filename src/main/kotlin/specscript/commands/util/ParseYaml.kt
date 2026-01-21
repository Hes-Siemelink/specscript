package specscript.commands.util

import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import specscript.util.Yaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ValueNode

object ParseYaml : CommandHandler("Parse Yaml", "core/util"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        return Yaml.parseIfPossible(data.asText())
    }
}