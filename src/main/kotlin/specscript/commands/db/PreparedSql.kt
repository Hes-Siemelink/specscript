package specscript.commands.db

import specscript.language.VARIABLE_REGEX
import specscript.language.getValue
import specscript.language.resolveVariablesInText
import specscript.util.Yaml
import specscript.util.toDisplayYaml
import specscript.util.toJsonNode
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.StringNode

/** A SQL statement with extracted prepared statement parameters. */
data class PreparedSql(val sql: String, val parameters: List<Any?>)

/** Regex matching a variable reference wrapped in single quotes: '${...}' */
private val QUOTED_VARIABLE = Regex("'(\\$\\{[^}]+})'")

/**
 * Extract prepared statement parameters from a SQL string containing SpecScript variables.
 *
 * Quoted variable references ('${var}') become ? placeholders with the resolved value as a parameter.
 * Unquoted variable references (${var}) are resolved inline as text.
 */
fun prepareSql(sql: String, variables: Map<String, JsonNode>): PreparedSql {
    val parameters = mutableListOf<Any?>()

    // First pass: replace quoted variables with ? placeholders and collect parameter values
    val withPlaceholders = QUOTED_VARIABLE.replace(sql) { match ->
        val variableRef = match.groupValues[1]
        val varName = VARIABLE_REGEX.find(variableRef)!!.groupValues[1]
        val value = getValue(varName, variables)
        parameters.add(jsonToJdbc(value))
        "?"
    }

    // Second pass: resolve any remaining unquoted variable references inline
    val resolved = if (VARIABLE_REGEX.containsMatchIn(withPlaceholders)) {
        resolveVariablesInText(withPlaceholders, variables)
    } else {
        withPlaceholders
    }

    return PreparedSql(resolved, parameters)
}

/** Convert a JsonNode to a value suitable for a JDBC prepared statement parameter. */
fun jsonToJdbc(node: JsonNode): Any? = when {
    node.isNull -> null
    node.isNumber -> node.numberValue()
    node.isBoolean -> if (node.booleanValue()) 1 else 0
    node.isString -> node.stringValue()
    else -> node.toDisplayYaml()
}

/** Convert a JDBC value to a Jackson JsonNode without YAML parsing. */
fun jdbcToJson(value: Any?): JsonNode = when (value) {
    null -> StringNode("")
    is Number -> value.toJsonNode()
    is Boolean -> BooleanNode.valueOf(value)
    is String -> Yaml.parseIfPossible(value)
    else -> StringNode(value.toString())
}
