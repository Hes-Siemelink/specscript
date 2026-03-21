package specscript.language

import specscript.util.JsonProcessor
import specscript.util.toDisplayYaml
import tools.jackson.core.JsonPointer
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode

val VARIABLE_REGEX = Regex("\\$\\{([^}]+)}")

fun JsonNode.resolveVariables(variables: Map<String, JsonNode>): JsonNode {
    return VariableResolver(variables).process(this)
}

private class VariableResolver(val variables: Map<String, JsonNode>) : JsonProcessor() {

    override fun processText(node: StringNode): JsonNode {

        // Single variable reference will return full content of variable as node
        val singleVariableMatch = VARIABLE_REGEX.matchEntire(node.stringValue())
        if (singleVariableMatch != null) {
            val varName = singleVariableMatch.groupValues[1]
            return getValue(varName, variables)
        }

        // One or more variables mixed in text are replaced with text values
        // Only replace the node is there is a variable in it
        if (VARIABLE_REGEX.containsMatchIn(node.stringValue())) {
            return StringNode(resolveVariablesInText(node.stringValue(), variables))
        }

        return node
    }
}

fun resolveVariablesInText(raw: String, variables: Map<String, JsonNode>): String {
    val replaced = VARIABLE_REGEX.replace(raw) {
        getValue(it.groupValues[1], variables).toDisplayYaml()
    }
    return replaced
}

fun getValue(varName: String, variables: Map<String, JsonNode>): JsonNode {

    val variableWithPath: VariableWithPath = splitIntoVariableAndPath(varName)

    if (!variables.containsKey(variableWithPath.name)) {
        // FIXME Produces message: "Unknown variable 'greeting' in ${greeting}"
        throw SpecScriptException("Unknown variable \${${variableWithPath.name}}")
    }

    val value = variables[variableWithPath.name]!!

    return if (variableWithPath.path == null) {
        value
    } else {
        val jsonPointer = toJsonPointer(variableWithPath.path)
        value.at(jsonPointer)
    }
}

fun splitIntoVariableAndPath(varName: String): VariableWithPath {

    val split = Regex("(.*?)([\\[.].*\$)")
    val match = split.find(varName) ?: return VariableWithPath(varName, null)

    return VariableWithPath(match.groupValues[1], match.groupValues[2])
}

fun toJsonPointer(jsonPath: String): JsonPointer {
    val segments = parsePath(jsonPath)
    val pointer = segments.joinToString("") { "/" + escapeJsonPointerSegment(it) }
    return JsonPointer.compile(pointer)
}

/** Parse a SpecScript path into segments, handling dot notation, `[N]` array indexes, and `["key"]` bracket notation. */
private fun parsePath(path: String): List<String> {
    val segments = mutableListOf<String>()
    val current = StringBuilder()
    var i = 0
    while (i < path.length) {
        when {
            path[i] == '.' -> {
                if (current.isNotEmpty()) segments.add(current.toString())
                current.clear()
                i++
            }
            path[i] == '[' -> {
                if (current.isNotEmpty()) {
                    segments.add(current.toString())
                    current.clear()
                }
                if (i + 1 < path.length && path[i + 1] == '"') {
                    // Quoted bracket notation: ["key"]
                    val closeQuote = path.indexOf('"', i + 2)
                    segments.add(path.substring(i + 2, closeQuote))
                    i = closeQuote + 2 // skip closing "]
                } else {
                    // Numeric index: [0]
                    val end = path.indexOf(']', i)
                    segments.add(path.substring(i + 1, end))
                    i = end + 1
                }
            }
            else -> {
                current.append(path[i])
                i++
            }
        }
    }
    if (current.isNotEmpty()) segments.add(current.toString())
    return segments
}

/** Escape a segment for JSON Pointer per RFC 6901: `~` → `~0`, `/` → `~1`. */
private fun escapeJsonPointerSegment(segment: String): String {
    return segment.replace("~", "~0").replace("/", "~1")
}

data class VariableWithPath(val name: String, val path: String?)

