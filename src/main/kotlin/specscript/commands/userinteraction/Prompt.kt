package specscript.commands.userinteraction

import specscript.commands.testing.getAnswers
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import specscript.language.types.ParameterData
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

/**
 * Asks user through simple text prompt
 */
object Prompt : CommandHandler("Prompt", "core/user-interaction"), ValueHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode {
        return UserPrompt.prompt(data.stringValue(), answers = context.getAnswers())
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val parameterData = data.toDomainObject(ParameterData::class)

        // Only ask if condition is true
        parameterData.conditionValid() || return null

        return parameterData.prompt(answers = context.getAnswers())
    }
}
