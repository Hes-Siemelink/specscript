package specscript.commands.datamanipulation

import specscript.language.*
import specscript.util.toArrayNode
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.NumericNode
import tools.jackson.databind.node.ObjectNode

object Sort : CommandHandler("Sort", "core/data-manipulation"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val items = data["items"]
            ?: context.output
            ?: throw CommandFormatException("Specify 'items' or make sure \${output} is set.")
        if (items !is ArrayNode) throw CommandFormatException("items should be an array")
        val sortField = data.getTextParameter("by")

        val sorted = items.sortedWith(NodeComparator(sortField))

        return sorted.toArrayNode()
    }
}

private class NodeComparator(val field: String) : Comparator<JsonNode> {

    override fun compare(node1: JsonNode, node2: JsonNode): Int {

        val value1 = node1[field] ?: return 0
        val value2 = node2[field] ?: return 0

        return if (value1 is NumericNode && value2 is NumericNode) {
            value1.asInt() - value2.asInt()
        } else {
            value1.asString().compareTo(value2.asString())
        }
    }
}
