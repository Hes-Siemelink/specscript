package specscript.commands.userinteraction

import specscript.language.*
import specscript.language.types.ParameterData
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

/**
 * Asks multiple questions at once
 */
object PromptObject : CommandHandler("Prompt object", "core/user-interaction"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val answers = data.objectNode()

        // Temporary variables that will hold the contents of the entries so later ones can refer to previous ones
        val variables = context.variables.toMutableMap()

        for ((name, parameterData) in data.properties()) {

            // Resolve variables
            val parameter = parameterData.resolveVariables(variables).toDomainObject(ParameterData::class)

            // Only ask if condition is true
            parameter.conditionValid() || continue

            // Ask user
            val answer = parameter.prompt(name)

            // Add answer to result and to list of variables
            answers.set(name, answer)
            variables[name] = answer
        }

        return answers
    }
}