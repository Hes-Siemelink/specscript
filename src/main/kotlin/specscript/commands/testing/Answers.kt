package specscript.commands.testing

import specscript.commands.userinteraction.ANSWERS_SESSION_KEY
import specscript.commands.userinteraction.AnswersMap
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.SpecScriptException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

/**
 * Records answers to be replayed in test cases for user input commands.
 * Answers are stored in the script context session, not in global state.
 */
object Answers : CommandHandler("Answers", "core/testing"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val answers = context.getOrCreateAnswers()

        data.properties().forEach {
            answers[it.key] = it.value
        }

        return null
    }
}

fun ScriptContext.getAnswers(): AnswersMap {
    @Suppress("UNCHECKED_CAST")
    return session[ANSWERS_SESSION_KEY] as? MutableMap<String, JsonNode> ?: emptyMap()
}

fun ScriptContext.hasRecordedAnswer(question: String): Boolean {
    return getAnswers().containsKey(question)
}

fun ScriptContext.getRecordedAnswer(question: String): JsonNode {
    return getAnswers()[question] ?: throw SpecScriptException("No recorded answer for: $question")
}

private fun ScriptContext.getOrCreateAnswers(): MutableMap<String, JsonNode> {
    @Suppress("UNCHECKED_CAST")
    return session.getOrPut(ANSWERS_SESSION_KEY) { mutableMapOf<String, JsonNode>() } as MutableMap<String, JsonNode>
}
