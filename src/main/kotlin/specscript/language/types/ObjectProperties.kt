package specscript.language.types

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import specscript.cli.infoString
import tools.jackson.databind.JsonNode

interface ObjectDefinition {
    val properties: Map<String, PropertyDefinition>
}

fun ObjectDefinition.toDisplayString(): String {

    val builder = StringBuilder()

    val width = properties.maxOf { it.key.length } + 2
    properties.forEach {
        val key = buildString {
            append("--")
            append(it.key)
            if (it.value.shortOption != null) {
                append(", -")
                append(it.value.shortOption)
            }
        }
        builder.append("  ")
        builder.append(infoString(key, it.value.description ?: "", width))
        builder.appendLine()
    }

    return builder.toString()
}

class ObjectProperties : ObjectDefinition {

    @JsonAnyGetter
    @JsonAnySetter
    override val properties: MutableMap<String, PropertySpecification> = LinkedHashMap()

    fun validate(data: JsonNode): List<String> {
        val messages = mutableListOf<String>()

        for ((field, definition) in properties) {
            if (data.has(field)) {
                val parameter = properties[field]
                val value = data[field]
                parameter?.type?.let { type ->
                    messages.addAll(type.validate(value))
                }
            } else if (!definition.optional) {
                messages.add("Missing property: $field")
            }
        }

        return messages
    }

    companion object {
        operator fun invoke(properties: Map<String, PropertySpecification>): ObjectProperties {
            return ObjectProperties().also { it.properties.putAll(properties) }
        }
    }
}
