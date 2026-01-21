package specscript.commands.datamanipulation

import specscript.language.*
import specscript.util.toDisplayYaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.TextNode

object Replace : CommandHandler("Replace", "core/data-manipulation"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {

        val text = data.getParameter("text")
        val source = data["in"] ?: context.output ?: throw SpecScriptCommandError(
            "Replace needs 'in' parameter or non-null output variable."
        )
        val replacement = data.getParameter("with")

        val result = replace(source, text, replacement)

        return result ?: data
    }

    private fun replace(
        source: JsonNode,
        part: JsonNode,
        replacement: JsonNode
    ): JsonNode? {

        return when (source) {
            is TextNode -> {
                replaceText(source.textValue(), part, replacement)
            }

            is ArrayNode -> {
                replaceArray(source, part, replacement)
            }

            is ObjectNode -> {
                replaceObject(source, part, replacement)
            }

            else -> null
        }
    }

    private fun replaceText(source: String, part: JsonNode, replaceWith: JsonNode): JsonNode {
        if (part !is TextNode) {
            throw CommandFormatException("'Replace.find' may contain text only")
        }

        val replacementText = replaceWith.toDisplayYaml()
        val replacement = source.replace(part.textValue(), replacementText)

        return TextNode(replacement)
    }

    private fun replaceArray(source: ArrayNode, part: JsonNode, replaceWith: JsonNode): JsonNode {
        val replacement = ArrayNode(JsonNodeFactory.instance)
        for (node in source) {
            replacement.add(replace(node, part, replaceWith))
        }
        return replacement
    }

    private fun replaceObject(source: ObjectNode, part: JsonNode, replaceWith: JsonNode): JsonNode {
        val replacement = source.objectNode()
        for (field in source.fields()) {
            replacement.set<JsonNode>(field.key, replace(field.value, part, replaceWith))
        }
        return replacement
    }
}