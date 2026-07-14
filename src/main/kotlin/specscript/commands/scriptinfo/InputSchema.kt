package specscript.commands.scriptinfo

import com.fasterxml.jackson.annotation.JsonAnyGetter
import specscript.commands.toCondition
import specscript.commands.userinteraction.resolveValue
import specscript.language.*
import specscript.language.types.ObjectDefinition
import specscript.language.types.ParameterData
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

object InputSchema : CommandHandler("Input schema", "core/script-info"),
    ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {
        val schemaData = toInputData(data)

        populateInputVariables(context, schemaData)

        return context.getInputVariables()
    }

    fun toInputData(data: ObjectNode): InputSchemaData {
        val propertiesNode = data.get("properties") as? ObjectNode ?: return InputSchemaData()

        val parameters = mutableMapOf<String, ParameterData>()
        for ((name, propNode) in propertiesNode.properties()) {
            parameters[name] = propNode.toDomainObject(ParameterData::class)
        }

        return InputSchemaData(parameters)
    }
}

/**
 * Populates the input variables from a set of property definitions.
 *
 * Resolution order per property: already-set input → environment variable → recorded answer →
 * interactive prompt (default as hint) → default value → error.
 */
fun populateInputVariables(context: ScriptContext, input: ObjectDefinition) {

    for ((name, info) in input.properties.entries) {

        // Already provided as input
        if (context.getInputVariables().has(name)) {
            // Copy variable to top level
            context.variables[name] = context.getInputVariables()[name]
            continue
        }

        // Skip if condition is not valid
        if (!conditionValid(info.condition, context)) {
            continue
        }

        // Resolve from environment variable, recorded answer, interactive prompt or default
        val answer: JsonNode = info.resolveValue(name, context, checkEnv = true)
            ?: throw MissingInputException("No value provided for: $name", name, input)

        context.getInputVariables().set(name, answer)
        context.variables[name] = answer
    }
}

private fun conditionValid(condition: JsonNode?, context: ScriptContext): Boolean {
    condition ?: return true

    return condition.resolveVariables(context.variables).toCondition().isTrue()
}

data class InputSchemaData(
    @get:JsonAnyGetter
    override val properties: Map<String, ParameterData> = mutableMapOf()
) : ObjectDefinition
