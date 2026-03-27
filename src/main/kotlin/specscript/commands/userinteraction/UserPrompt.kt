package specscript.commands.userinteraction

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptCheckboxObject
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptInputPassword
import com.github.kinquirer.components.promptListObject
import com.github.kinquirer.core.Choice
import specscript.language.SpecScriptException
import specscript.util.Json
import specscript.util.toDisplayYaml
import specscript.util.toJsonNode
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode

typealias AnswersMap = Map<String, JsonNode>

const val ANSWERS_SESSION_KEY = "answers"

/**
 * User prompt functions for interactive input.
 *
 * When the answers map is non-empty, answers are looked up from the map and
 * simulated output is printed (test mode). When empty, real terminal prompts
 * are shown via KInquirer.
 */
object UserPrompt {

    fun prompt(message: String, default: String = "", password: Boolean = false, answers: AnswersMap = emptyMap()): JsonNode {
        if (answers.isNotEmpty()) {
            return testPrompt(message, default, password, answers)
        }
        return realPrompt(message, default, password)
    }

    fun select(message: String, choices: List<Choice<JsonNode>>, multiple: Boolean = false, answers: AnswersMap = emptyMap()): JsonNode {
        if (answers.isNotEmpty()) {
            return testSelect(message, choices, multiple, answers)
        }
        return realSelect(message, choices, multiple)
    }

    // -- Real prompts (KInquirer) --

    private fun realPrompt(message: String, default: String, password: Boolean): JsonNode {
        val answer = if (password) {
            KInquirer.promptInputPassword(message, default)
        } else {
            KInquirer.promptInput(message, default)
        }
        return StringNode(answer)
    }

    private fun realSelect(message: String, choices: List<Choice<JsonNode>>, multiple: Boolean): JsonNode =
        if (multiple) {
            val answers = KInquirer.promptCheckboxObject(message, choices, minNumOfSelection = 1)
            answers.toJsonNode()
        } else {
            KInquirer.promptListObject(message, choices)
        }

    // -- Test prompts (simulated output from recorded answers) --

    private fun testPrompt(message: String, default: String, password: Boolean, answers: AnswersMap): JsonNode {
        val answer: JsonNode = answers[message] ?: if (default.isNotEmpty()) {
            StringNode(default)
        } else {
            StringNode("")
        }

        if (password) {
            println(KInquirer.renderInput(message, "********"))
        } else {
            println(KInquirer.renderInput(message, answer.toDisplayYaml()))
        }

        return answer
    }

    private fun testSelect(message: String, choices: List<Choice<JsonNode>>, multiple: Boolean, answers: AnswersMap): JsonNode {
        val selectedAnswer =
            answers[message] ?: throw IllegalStateException("No prerecorded answer for '$message'")

        if (multiple) {
            val set = selectedAnswer.toList().map { it.stringValue() }
            val selection = choices.filter {
                set.contains(it.displayName)
            }
            val result = Json.newArray()
            selection.forEach { result.add(it.data) }

            println(KInquirer.renderInput(message, choices, selection))

            return result

        } else {
            val selection = choices.find {
                selectedAnswer.stringValue() == it.displayName
            } ?: throw SpecScriptException("Prerecorded choice '$selectedAnswer' not found in provided list.")

            println(KInquirer.renderInput(message, choices, listOf(selection)))

            return selection.data
        }
    }
}


private fun KInquirer.renderInput(message: String, answer: String = ""): String = buildString {

    append("? ")
    append(message)
    append(" ")

    if (answer.isNotEmpty()) {
        append(answer)
    }
}

// ? Select ingredients
//  ❯ ◉ Apple
//    ◯ Banana
//    ◯ Cake
//    ◯ Drizzle
private fun KInquirer.renderInput(
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
