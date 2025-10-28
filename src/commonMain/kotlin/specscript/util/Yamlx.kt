package specscript.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import net.mamoe.yamlkt.Yaml

/**
 * Yaml parse methods with kotlinx.serialization.
 */
object Yamlx {
    private val yaml = Yaml
    private val json = Json // compact

    /** Parses a single YAML document. */
    fun parse(content: String): JsonElement =
        yaml.decodeFromString(JsonElement.serializer(), content)

    /** Parses multiple YAML documents separated '---'.*/
    fun parseAll(multi: String): List<JsonElement> = multi
        .split(Regex("^---\\s*$", RegexOption.MULTILINE))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { parse(it) }

    /** Try to parse; if parsing fails return the original text as a JsonPrimitive. */
    fun parseIfPossible(content: String?): JsonElement = when {
        content == null -> JsonPrimitive("")
        content.isBlank() -> JsonPrimitive("")
        else -> try {
            parse(content)
        } catch (_: Throwable) {
            JsonPrimitive(content)
        }
    }

    fun <T> decode(element: JsonElement, serializer: KSerializer<T>): T =
        json.decodeFromJsonElement(serializer, element)

    fun <T> encode(value: T, serializer: KSerializer<T>): String {
        val tree = json.encodeToJsonElement(serializer, value)
        return tree.toYamlString()
    }
}

fun JsonElement.toYamlString(): String {
    return Yaml.encodeToString(JsonElement.serializer(), this)
}


fun JsonElement.toJsonString(pretty: Boolean = true): String {
    return if (pretty) {
        Jsonx.pretty.encodeToString(JsonElement.serializer(), this)
    } else {
        Json.encodeToString(JsonElement.serializer(), this)
    }
}
