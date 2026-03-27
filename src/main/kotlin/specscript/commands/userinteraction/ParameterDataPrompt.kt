package specscript.commands.userinteraction

import com.github.kinquirer.core.Choice
import specscript.language.CommandFormatException
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


fun PropertyDefinition.prompt(label: String? = null, answers: AnswersMap = emptyMap()): JsonNode {
    val message = description ?: label ?: ""

    return when {
        enum != null && select == "single" ->
            promptChoice(message, answers = answers)

        enum != null && select == "multiple" ->
            promptChoice(message, multiple = true, answers = answers)

        secret ->
            promptText(message, password = true, answers = answers)

        type != null ->
            promptByType(message, type!!, answers)

        else ->
            promptText(message, answers = answers)
    }
}

private fun PropertyDefinition.promptText(message: String, password: Boolean = false, answers: AnswersMap = emptyMap()): JsonNode {
    return UserPrompt.prompt(message, default?.asString() ?: "", password, answers)
}

private fun PropertyDefinition.promptBoolean(message: String, answers: AnswersMap = emptyMap()): JsonNode {

    val answer = UserPrompt.prompt(message, default?.asString() ?: "", answers = answers)

    return if (answer.stringValue() == "true") TRUE
    else FALSE
}

private fun PropertyDefinition.promptChoice(message: String, multiple: Boolean = false, answers: AnswersMap = emptyMap()): JsonNode {

    val choices = enum?.map { choiceData ->
        if (displayProperty == null) {
            Choice(choiceData.toDisplayYaml(), choiceData)
        } else {
            Choice(choiceData[displayProperty].stringValue(), choiceData)
        }
    } ?: emptyList()

    val answer = UserPrompt.select(message, choices, multiple, answers)

    return answer.onlyWith(valueProperty)
}

private fun PropertyDefinition.promptByType(message: String, type: TypeSpecification, answers: AnswersMap = emptyMap()): JsonNode {

    // Primitive types
    when (type.name) {
        "boolean" -> return promptBoolean(message, answers)
        "string" -> return promptText(message, answers = answers)
        // TODO support other primitive types
    }


    // Primitive types

    return when {
        type.properties != null -> type.properties.promptObject(answers)
        type.listOf != null -> promptList(message, type.listOf, answers)
        type.base != null -> {
            when (type.base) {
                "boolean" -> promptBoolean(message, answers)
                "string" -> promptText(message, answers = answers)
                else -> throw CommandFormatException("Base type not supported: ${type.base}")
            }
        }

        else -> throw CommandFormatException("Type not supported: $type")
    }
}

private fun ObjectProperties.promptObject(answers: AnswersMap = emptyMap()): JsonNode {

    val result = Json.newObject()

    // Temporary variables that will hold the contents of the entries so later ones can refer to previous ones
    val variables = mutableMapOf<String, JsonNode>()

    for ((name, parameter) in properties) {

        // Only ask if condition is true
        parameter.conditionValid() || continue

        // Ask user
        val answer = parameter.prompt(name, answers)

        // Add answer to result and to list of variables
        result.set(name, answer)
        variables[name] = answer
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


private fun promptList(message: String, type: TypeSpecification, answers: AnswersMap = emptyMap()): ArrayNode {
    val list = Json.newArray()

    val name = type.name ?: "item"
    val add = StringNode("Add new $name")
    val done = StringNode("Done")

    while (true) {

        val choice = ParameterData(
            enum = listOf(add, done)
        ).promptChoice(message, answers = answers)

        if (choice == done) {
            break
        }

        val item = ParameterData(
            type = type
        ).promptByType("Enter new item", type, answers)
        list.add(item)
    }

    return list
}
