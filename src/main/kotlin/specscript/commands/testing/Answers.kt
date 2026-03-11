package specscript.commands.testing

import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.SpecScriptException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

/**
 * Records answers to be replayed in test cases for user input commands.
 */
object Answers : CommandHandler("Answers", "core/testing"), ObjectHandler {

    val recordedAnswers = mutableMapOf<String, JsonNode>()

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        data.properties().forEach {
            recordedAnswers[it.key] = it.value
        }

        return null
    }

    fun hasRecordedAnswer(question: String): Boolean {
        return recordedAnswers.containsKey(question)
    }

    fun getRecordedAnswer(question: String): JsonNode {
        return recordedAnswers[question] ?: throw SpecScriptException("No recorded answer for: $question")

    }
}
