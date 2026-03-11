package specscript.language

import specscript.util.JsonProcessor
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

fun eval(data: JsonNode, context: ScriptContext): JsonNode {
    return Evaluator(context).process(data)
}

private class Evaluator(val context: ScriptContext) : JsonProcessor() {

    override fun processObject(node: ObjectNode): JsonNode {

        for ((key, data) in node.properties()) {
            val evaluatedData = process(data)
            node.set(key, evaluatedData)

            if (key.startsWith("/")) {
                val name = key.substring(1)
                val handler = context.getCommandHandler(name)
                val result = runCommand(handler, evaluatedData, context)
                return result ?: StringNode("")
            }
        }

        return node
    }
}