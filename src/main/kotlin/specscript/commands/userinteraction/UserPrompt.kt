package specscript.commands.userinteraction

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptCheckboxObject
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptInputPassword
import com.github.kinquirer.components.promptListObject
import com.github.kinquirer.core.Choice
import specscript.util.Json
import specscript.util.toDisplayYaml
import specscript.util.toJsonNode
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode

typealias AnswersMap = Map<String, JsonNode>

const val ANSWERS_SESSION_KEY = "answers"

const val NON_INTERACTIVE_PLACEHOLDER = "[default answer in non-interactive mode]"

/**
 * User prompt functions for interactive input.
 *
 * Each function resolves a recorded answer or an interactive prompt, returning `null` when neither
 * applies. The default value and missing-value policy are applied by the caller (the resolver).
 */
object UserPrompt {

    fun prompt(
        message: String,
        defaultHint: String = "",
        password: Boolean = false,
        answers: AnswersMap = emptyMap(),
        interactive: Boolean = false
    ): JsonNode? {

        // Use prerecorded answer
        val answer = answers[message]
        if (answer != null) {
            println(PromptDisplayReference.input(message, answer.toDisplayYaml(), password))
            return answer
        }

        // Interactive prompt, if allowed (default value is a hint, pre-filled in the prompt)
        if (interactive) {
            return interactivePrompt(message, defaultHint, password)
        }

        return null
    }

    fun select(
        message: String,
        choices: List<Choice<JsonNode>>,
        multiple: Boolean = false,
        answers: AnswersMap = emptyMap(),
        interactive: Boolean = false
    ): JsonNode? {

        // Use prerecorded answer
        val answer = answers[message]
        if (answer != null) {
            return simulatedSelect(message, choices, multiple, answer)
        }

        // Interactive prompt, if allowed
        if (interactive) {
            return interactiveSelect(message, choices, multiple)
        }

        return null
    }

    // -- Real prompts (KInquirer) --

    private fun interactivePrompt(message: String, default: String, password: Boolean): JsonNode {
        val answer = if (password) {
            KInquirer.promptInputPassword(message, default)
        } else {
            KInquirer.promptInput(message, default)
        }
        return StringNode(answer)
    }

    private fun interactiveSelect(message: String, choices: List<Choice<JsonNode>>, multiple: Boolean): JsonNode =
        if (multiple) {
            val answers = KInquirer.promptCheckboxObject(message, choices, minNumOfSelection = 1)
            answers.toJsonNode()
        } else {
            KInquirer.promptListObject(message, choices)
        }

    // -- Test prompts (simulated output from recorded answers) --

    private fun simulatedSelect(
        message: String,
        choices: List<Choice<JsonNode>>,
        multiple: Boolean,
        selectedAnswer: JsonNode
    ): JsonNode {

        val set = if (multiple) {
            selectedAnswer.toList().map { it.stringValue() }
        } else {
            setOf(selectedAnswer.stringValue())
        }

        val selection = choices.filter {
            set.contains(it.displayName)
        }
        val result = Json.newArray()
        selection.forEach { result.add(it.data) }

        println(PromptDisplayReference.choice(message, choices, selection))

        return if (multiple) result else result.first()
    }
}

object PromptDisplayReference {

    fun input(message: String, answer: String = "", password: Boolean): String = buildString {

        val displayAnswer = if (password) "********" else answer

        append("? ")
        append(message)
        append(" ")

        if (displayAnswer.isNotEmpty()) {
            append(displayAnswer)
        }
    }

    // ? Select ingredients
    //  ❯ ◉ Apple
    //    ◯ Banana
    //    ◯ Cake
    //    ◯ Drizzle
    fun choice(
        message: String,
        choices: List<Choice<JsonNode>>,
        answer: List<Choice<JsonNode>>
    ): String =
        buildString {
            append("? ")
            append(message)
            append(" \n")
            var first = true
            choices.forEach { choice ->
                if (answer.contains(choice)) {
                    if (first) {
                        append(" ❯ ◉ ")
                        first = false
                    } else {
                        append("   ◉ ")
                    }
                } else {
                    append("   ◯ ")
                }
                append(choice.displayName)
                append("\n")
            }
        }

}
