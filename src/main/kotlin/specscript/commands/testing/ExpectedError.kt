package specscript.commands.testing

import specscript.language.*
import specscript.util.toDisplayYaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

object ExpectedError :
    CommandHandler("Expected error", "core/testing"),
    ErrorHandler,
    ValueHandler,
    ArrayHandler,
    ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        if (context.error == null) {
            throw MissingExpectedError(data.textValue())
        }

        context.error = null

        return null
    }

    override fun execute(data: ArrayNode, context: ScriptContext): JsonNode? {
        throw CommandFormatException("Arrays are not allowed in 'Expected error'")
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        for (key in data.propertyNames()) {
            if (key == "any" || key == context.error?.error?.type) {
                context.error = null
                return null
            }
        }

        if (context.error == null) {
            throw MissingExpectedError(data.toDisplayYaml())
        } else {
            throw MissingExpectedError("${data.toDisplayYaml()}\nGot instead: ${context.error?.error} ")
        }
    }
}

class MissingExpectedError(message: String) : SpecScriptCommandError(message)