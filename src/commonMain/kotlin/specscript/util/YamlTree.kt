package specscript.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import net.mamoe.yamlkt.Yaml

/**
 * Multiplatform YAML parsing utilities producing kotlinx.serialization JsonElement trees.
 * Transitional layer while migrating away from Jackson JsonNode.
 */
object YamlTree {
    // Default instance (can tune later: strictness, unknown keys, etc.)
    private val yaml = Yaml

    /** Parse a single YAML document into a JsonElement tree. */
    fun parse(content: String): JsonElement =
        yaml.decodeFromString(JsonElement.serializer(), content)

    /**
     * Parse zero or more YAML documents separated by '---' lines.
     * Empty documents are skipped.
     */
    fun parseAll(multiDocContent: String): List<JsonElement> {
        if (multiDocContent.isBlank()) return emptyList()
        return multiDocContent
            .split(Regex("^---\\s*$", RegexOption.MULTILINE))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { parse(it) }
    }

    /** Attempt to parse; on failure return a JsonPrimitive with original content. */
    fun parseIfPossible(content: String?): JsonElement {
        content ?: return JsonPrimitive("")
        return try {
            parse(content)
        } catch (_: Throwable) {
            JsonPrimitive(content)
        }
    }
}

// Display helpers
fun JsonElement.toDisplayJson(pretty: Boolean = true): String =
    if (pretty) Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), this)
    else Json.encodeToString(JsonElement.serializer(), this)

/** YAML display using yamlkt encode support. */
fun JsonElement.toDisplayYaml(): String =
    Yaml.encodeToString(JsonElement.serializer(), this).trim()
