package specscript.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import net.mamoe.yamlkt.Yaml

/**
 * Multiplatform YAML utility (schemaless) using yamlkt + JsonElement.
 * Mirrors a subset of the original Jackson-based Yaml helper without file/resource I/O.
 * File / resource helpers will be added per-platform later.
 */
object Yamlx {
    private val yaml = Yaml
    private val json = Json // compact

    /** Parse a single YAML document into a JsonElement tree. */
    fun parse(content: String): JsonElement =
        yaml.decodeFromString(JsonElement.serializer(), content)

    /** Parse multiple YAML documents from one string separated by '---' lines. */
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

    /** Generic decode from a JsonElement produced by parse(). */
    fun <T> decode(element: JsonElement, serializer: KSerializer<T>): T =
        json.decodeFromJsonElement(serializer, element)

    /** Encode any serializable value back to YAML via intermediate JsonElement. */
    fun <T> encode(value: T, serializer: KSerializer<T>): String {
        val tree = json.encodeToJsonElement(serializer, value)
        return Yaml.encodeToString(JsonElement.serializer(), tree).trim()
    }
}

/** Display helpers */
fun JsonElement.toYamlString(): String = Yaml.encodeToString(JsonElement.serializer(), this).trim()
fun JsonElement.toJsonString(pretty: Boolean = true): String =
    if (pretty) Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), this)
    else Json.encodeToString(JsonElement.serializer(), this)

