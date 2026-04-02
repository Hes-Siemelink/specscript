package specscript.commands.controlflow

import specscript.language.*
import specscript.util.Json
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.*

object ForEach : CommandHandler("For each", "core/control-flow"), ObjectHandler, DelayedResolver {

    private val FOR_EACH_VARIABLE = Regex(VARIABLE_REGEX.pattern + " in")

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {

        // Copy the data because we will modify it
        val body = data.deepCopy()

        val (loopVar, itemData) = removeLoopVariable(body)
            ?: Pair(
                "item",
                StringNode("\${output}") // Pass ${output} as a string to prevent resolution of variable syntax inside the data
            ).apply {
                checkNotNull(context.output) { "For each without loop variable takes items from  \${output}, but \${output} is null" }
            }


        val items = itemData.resolve(context)

        return context.withScopedVariable(loopVar) {
            iterateOver(items, body, loopVar, context)
        }
    }

    private fun removeLoopVariable(data: ObjectNode): Pair<String, JsonNode>? {
        val first = data.propertyNames().first()
        val match = FOR_EACH_VARIABLE.matchEntire(first) ?: return null
        val items = data.remove(first)

        return Pair(match.groupValues[1], items)
    }

    private fun iterateOver(items: JsonNode, body: ObjectNode, loopVar: String, context: ScriptContext): JsonNode {
        val output: JsonNode = if (items is ArrayNode) body.arrayNode() else body.objectNode()

        for (item in items.enumerateForEach()) {
            context.variables[loopVar] = item

            // Copy the body statement because variable resolution is in-place and modifies the data
            val copy = body.deepCopy()
            val result = copy.run(context)

            result?.let {
                when (output) {
                    is ArrayNode -> output.add(result)
                    is ObjectNode -> output.set(item["key"].stringValue(), result)
                    else -> {}
                }
            }
        }

        return output
    }
}

private fun JsonNode.enumerateForEach(): ArrayNode {
    when (this) {
        is ArrayNode -> return this
        is ValueNode -> return Json.newArray().add(this)
        is ObjectNode -> {
            val array = Json.newArray()
            for (field in properties()) {
                val obj: ObjectNode = array.objectNode()
                obj.set("key", array.stringNode(field.key))
                obj.set("value", field.value)
                array.add(obj)
            }
            return array
        }

        is MissingNode -> return Json.newArray()
    }
    throw AssertionError("Unsupported node type ${this.javaClass}")
}