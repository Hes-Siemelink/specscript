package specscript.commands.scriptinfo

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import com.fasterxml.jackson.module.kotlin.contains
import specscript.commands.testing.Answers
import specscript.commands.toCondition
import specscript.commands.userinteraction.prompt
import specscript.language.*
import specscript.language.types.ObjectDefinition
import specscript.language.types.ParameterData
import specscript.language.types.TypeSpecification
import specscript.language.types.resolve
import specscript.util.toDomainObject

object InputParameters : CommandHandler("Input parameters", "core/script-info"),
    ObjectHandler, ValueHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {
        val inputData = data.toDomainObject(InputParameterData::class)

        populateInputVariables(context, inputData)

        return context.getInputVariables()
    }

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        val type = TypeSpecification(data.textValue())

        val resolvedType = type.resolve(context.types).definition

        if (resolvedType.properties != null) {
            populateInputVariables(context, resolvedType.properties)
        } else {
            // TODO handle array and simple types
        }

        return context.getInputVariables()
    }

    fun populateInputVariables(
        context: ScriptContext,
        input: ObjectDefinition
    ) {

        for ((name, info) in input.properties.entries) {

            // Already exists
            if (context.getInputVariables().contains(name)) {
                // Copy variable to top level
                context.variables[name] = context.getInputVariables()[name]
                continue
            }

            // Skip if condition is not valid
            if (!conditionValid(info.condition, context)) {
                continue
            }

            // Find answer
            val question = info.description ?: name
            val answer: JsonNode = when {

                // Get default value
                info.default != null -> info.default!!

                // Get from answers
                Answers.hasRecordedAnswer(question) -> Answers.getRecordedAnswer(question)

                // Ask user
                context.interactive -> info.prompt(name)


                else -> throw MissingInputException(
                    "No value provided for: $name",
                    name,
                    input
                )
            }

            context.getInputVariables().set<JsonNode>(name, answer)
            context.variables[name] = answer
        }
    }

    fun handleInputType(
        context: ScriptContext,
        inputType: TypeSpecification
    ) {

    }
}

private fun conditionValid(condition: JsonNode?, context: ScriptContext): Boolean {
    condition ?: return true

    return condition.resolveVariables(context.variables).toCondition().isTrue()
}

data class InputParameterData(
    @get:JsonAnyGetter
    override val properties: Map<String, ParameterData> = mutableMapOf()
) : ObjectDefinition {
}

