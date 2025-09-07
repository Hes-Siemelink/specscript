package specscript.commands.testing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext

/**
 * Records answers to be replayed in test cases for user input commands.
 */
object Answers : CommandHandler("Answers", "core/testing"), ObjectHandler {

    val recordedAnswers = mutableMapOf<String, JsonNode>()

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        data.fields().forEach {
            recordedAnswers[it.key] = it.value
        }

        return null
    }
}
