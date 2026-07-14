package specscript.commands.userinteraction

import specscript.language.CommandHandler
import specscript.language.DelayedResolver
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.SpecScriptException
import specscript.language.ValueHandler
import specscript.language.resolveVariables
import specscript.language.types.ParameterData
import specscript.language.types.PropertyDefinition
import specscript.util.Json
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.databind.node.ValueNode

/**
 * Asks the user for input, described as a JSON Schema. The answer is returned as `${output}`.
 *
 * - A string is shorthand for a single text question.
 * - An object schema with `properties` asks several questions and returns an object.
 * - Any other object is a single property definition.
 */
object Prompt : CommandHandler("Prompt", "core/user-interaction"), ValueHandler, ObjectHandler, DelayedResolver {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode {
        val text = data.resolveVariables(context.variables).stringValue()
        return ParameterData(description = text).resolveOrPlaceholder(context)
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        // Object schema: ask each property and collect the answers into an object
        if (data.has("properties")) {
            return promptProperties(data.get("properties") as ObjectNode, context)
        }

        // Single property
        val param = data.resolveVariables(context.variables).toDomainObject(ParameterData::class)
        param.conditionValid() || return null
        return param.resolveOrPlaceholder(context)
    }

    private fun promptProperties(properties: ObjectNode, context: ScriptContext): ObjectNode {

        val result = Json.newObject()

        // Temporary variables so later questions can refer to earlier answers
        val variables = context.variables.toMutableMap()

        for ((name, propertyNode) in properties.properties()) {

            val param = propertyNode.resolveVariables(variables).toDomainObject(ParameterData::class)

            param.conditionValid() || continue

            val answer = param.resolveOrPlaceholder(context, name)

            result.set(name, answer)
            variables[name] = answer
        }

        return result
    }
}

private fun PropertyDefinition.resolveOrPlaceholder(context: ScriptContext, name: String? = null): JsonNode {

    resolveValue(name, context, checkEnv = false)?.let { return it }

    // Missing value in non-interactive mode: a selection has no safe default, free text falls back to a placeholder
    if (isChoice) {
        throw SpecScriptException("No value selected for '${question(name)}' and not in interactive mode")
    }
    return StringNode(NON_INTERACTIVE_PLACEHOLDER)
}
