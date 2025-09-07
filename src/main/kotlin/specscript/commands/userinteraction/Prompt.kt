package specscript.commands.userinteraction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import specscript.language.types.ParameterData
import specscript.util.toDomainObject

/**
 * Asks user through simple text prompt
 */
object Prompt : CommandHandler("Prompt", "core/user-interaction"), ValueHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode {
        return UserPrompt.prompt(data.textValue())
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val parameterData = data.toDomainObject(ParameterData::class)

        // Only ask if condition is true
        parameterData.conditionValid() || return null

        return parameterData.prompt()
    }
}