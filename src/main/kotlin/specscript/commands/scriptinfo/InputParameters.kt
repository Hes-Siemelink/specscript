package specscript.commands.scriptinfo

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import specscript.commands.testing.getAnswers
import specscript.commands.testing.getRecordedAnswer
import specscript.commands.testing.hasRecordedAnswer
import specscript.commands.toCondition
import specscript.commands.userinteraction.prompt
import specscript.language.*
import specscript.language.types.ObjectDefinition
import specscript.language.types.ParameterData
import specscript.language.types.TypeSpecification
import specscript.language.types.resolve
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.databind.node.ValueNode

object InputParameters : CommandHandler("Input parameters", "core/script-info"),
    ObjectHandler, ValueHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {
        val inputData = data.toDomainObject(InputParameterData::class)

        populateInputVariables(context, inputData)

        return context.getInputVariables()
    }

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        val type = TypeSpecification(data.stringValue())

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
            if (context.getInputVariables().has(name)) {
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
            val answers = context.getAnswers()
            val answer: JsonNode = when {

                // Get from environment variable
                info.env != null && System.getenv(info.env) != null -> StringNode(System.getenv(info.env))

                // Get default value
                info.default != null -> info.default!!

                // Get from answers
                context.hasRecordedAnswer(question) -> context.getRecordedAnswer(question)

                // Ask user
                context.interactive -> info.prompt(name, answers = answers)


                else -> throw MissingInputException(
                    "No value provided for: $name",
                    name,
                    input
                )
            }

            context.getInputVariables().set(name, answer)
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

class InputParameterData : ObjectDefinition {

    @JsonAnyGetter
    @JsonAnySetter
    override val properties: MutableMap<String, ParameterData> = LinkedHashMap()
}
