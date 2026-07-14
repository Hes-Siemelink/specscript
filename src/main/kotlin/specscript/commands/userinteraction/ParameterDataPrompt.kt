package specscript.commands.userinteraction

import com.github.kinquirer.core.Choice
import specscript.commands.testing.getAnswers
import specscript.language.CommandFormatException
import specscript.language.ScriptContext
import specscript.language.types.ObjectProperties
import specscript.language.types.ParameterData
import specscript.language.types.PropertyDefinition
import specscript.language.types.TypeSpecification
import specscript.util.Json
import specscript.util.toDisplayYaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.BooleanNode.FALSE
import tools.jackson.databind.node.BooleanNode.TRUE
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode


/**
 * Resolves the value for a single property.
 *
 * Order: environment variable (when [checkEnv]) → recorded answer → interactive prompt (with the
 * default as a hint) → default value. Returns `null` when nothing resolves; the caller decides
 * whether that is a placeholder (lenient text prompt) or an error.
 */
fun PropertyDefinition.resolveValue(
    name: String? = null,
    context: ScriptContext,
    checkEnv: Boolean = false
): JsonNode? {

    // Environment variable (input sources only)
    if (checkEnv && env != null) {
        System.getenv(env)?.let { return StringNode(it) }
    }

    // Recorded answer or interactive prompt
    val asked = ask(question(name), context.getAnswers(), context.interactive)
    if (asked != null) {
        return asked
    }

    // Fallback to default value
    return default
}

/** Whether a value must be selected (as opposed to freely typed). */
val PropertyDefinition.isChoice: Boolean get() = enum != null || isMultiple

/**
 * Asks a single question: recorded answer or interactive prompt. Returns `null` when neither
 * applies (non-interactive with no recorded answer).
 */
fun PropertyDefinition.ask(
    message: String,
    answers: AnswersMap = emptyMap(),
    interactive: Boolean = false
): JsonNode? {

    return when {
        isMultiple ->
            (items ?: return null).promptChoice(message, multiple = true, answers = answers, interactive = interactive)

        enum != null ->
            promptChoice(message, answers = answers, interactive = interactive)

        isPassword ->
            promptText(message, password = true, answers = answers, interactive = interactive)

        type != null && type!!.name != null && type!!.name != "string" ->
            promptByType(message, type!!, answers, interactive)

        else ->
            promptText(message, answers = answers, interactive = interactive)
    }
}

private fun PropertyDefinition.promptText(
    message: String,
    password: Boolean = false,
    answers: AnswersMap = emptyMap(),
    interactive: Boolean = false
): JsonNode? {
    return UserPrompt.prompt(message, default?.asString() ?: "", password, answers, interactive)
}

private fun PropertyDefinition.promptBoolean(
    message: String,
    answers: AnswersMap = emptyMap(),
    interactive: Boolean = false
): JsonNode? {

    val answer = UserPrompt.prompt(message, default?.asString() ?: "", answers = answers, interactive = interactive)
        ?: return null

    return if (answer.stringValue() == "true") TRUE
    else FALSE
}

private fun PropertyDefinition.promptChoice(
    message: String,
    multiple: Boolean = false,
    answers: AnswersMap = emptyMap(),
    interactive: Boolean = false
): JsonNode? {

    val choices = enum?.map { choiceData ->
        if (titleProperty == null) {
            Choice(choiceData.toDisplayYaml(), choiceData)
        } else {
            Choice(choiceData[titleProperty].stringValue(), choiceData)
        }
    } ?: emptyList()

    val answer = UserPrompt.select(message, choices, multiple, answers, interactive) ?: return null

    return answer.onlyWith(valueProperty)
}

private fun PropertyDefinition.promptByType(
    message: String,
    type: TypeSpecification,
    answers: AnswersMap = emptyMap(),
    interactive: Boolean = false
): JsonNode? {

    when (type.name) {
        "boolean" -> return promptBoolean(message, answers, interactive)
        "string" -> return promptText(message, answers = answers, interactive = interactive)
    }

    return when {
        type.properties != null -> type.properties.promptObject(answers, interactive)
        type.listOf != null -> promptList(message, type.listOf, answers, interactive)
        type.base != null -> {
            when (type.base) {
                "boolean" -> promptBoolean(message, answers, interactive)
                "string" -> promptText(message, answers = answers, interactive = interactive)

                else -> throw CommandFormatException("Base type not supported: ${type.base}")
            }
        }

        else -> throw CommandFormatException("Type not supported: $type")
    }
}

private fun ObjectProperties.promptObject(answers: AnswersMap = emptyMap(), interactive: Boolean = false): JsonNode {

    val result = Json.newObject()

    for ((name, parameter) in properties) {

        // Only ask if condition is true
        parameter.conditionValid() || continue

        // Ask user
        val answer = parameter.ask(parameter.question(name), answers, interactive) ?: parameter.default ?: continue

        result.set(name, answer)
    }

    return result
}

private fun JsonNode.onlyWith(field: String?): JsonNode {

    if (field == null) {
        return this
    }

    return when (this) {

        is ObjectNode -> this[field]

        is ArrayNode -> {
            val copy = arrayNode()
            for (item in this) {
                copy.add(item[field])
            }
            copy
        }

        else -> this
    }
}


private fun promptList(message: String, type: TypeSpecification, answers: AnswersMap = emptyMap(), interactive: Boolean = false): ArrayNode {
    val list = Json.newArray()

    val name = type.name ?: "item"
    val add = StringNode("Add new $name")
    val done = StringNode("Done")

    while (true) {

        val choice = ParameterData(
            enum = listOf(add, done)
        ).promptChoice(message, answers = answers, interactive = interactive)

        if (choice == null || choice == done) {
            break
        }

        val item = ParameterData(
            type = type
        ).promptByType("Enter new item", type, answers, interactive)
        if (item != null) list.add(item)
    }

    return list
}
