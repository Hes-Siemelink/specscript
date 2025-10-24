package specscript.util

import kotlinx.serialization.json.*

/**
 * Multiplatform JSON utility using kotlinx.serialization JsonElement tree.
 * Mirrors a subset of the Jackson-based Json utilities without conversions.
 */
object Jsonx {
    private val pretty = Json { prettyPrint = true }
    private val compact = Json

    fun newArray(): JsonArray = buildJsonArray {}
    fun newObject(): JsonObject = buildJsonObject {}

    fun newObject(key: String, value: String): JsonObject = buildJsonObject {
        put(key, JsonPrimitive(value))
    }

    fun newObject(map: Map<String, String>): JsonObject = buildJsonObject {
        map.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
    }

    fun parse(content: String): JsonElement = compact.parseToJsonElement(content)

    fun parseIfPossible(content: String?): JsonElement = when {
        content == null -> JsonPrimitive("")
        content.isBlank() -> JsonPrimitive("")
        else -> try {
            parse(content)
        } catch (_: Throwable) {
            JsonPrimitive(content)
        }
    }

    fun toPrettyString(element: JsonElement?): String =
        element?.let { pretty.encodeToString(JsonElement.serializer(), it) } ?: ""

    fun toCompactString(element: JsonElement?): String =
        element?.let { compact.encodeToString(JsonElement.serializer(), it) } ?: ""
}

// Extensions and helpers analogous to Jackson helpers

fun List<JsonElement>.toJsonx(): JsonArray = buildJsonArray { this@toJsonx.forEach { add(it) } }

fun JsonElement.asArray(): JsonArray = when (this) {
    is JsonArray -> this
    else -> buildJsonArray { add(this@asArray) }
}

fun JsonObject.add(vars: Map<String, String>): JsonObject = buildJsonObject {
    this@add.forEach { (k, v) -> put(k, v) }
    vars.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
}

// Processor analogue for transformation
abstract class JsonxProcessor {
    fun process(node: JsonElement): JsonElement = when (node) {
        is JsonObject -> processObject(node)
        is JsonArray -> processArray(node)
        is JsonPrimitive -> processPrimitive(node)
        JsonNull -> processNull()
    }

    open fun processArray(node: JsonArray): JsonElement = buildJsonArray {
        node.forEach { add(process(it)) }
    }

    open fun processObject(node: JsonObject): JsonElement = buildJsonObject {
        node.forEach { (k, v) -> put(k, process(v)) }
    }

    open fun processPrimitive(node: JsonPrimitive): JsonElement = node
    open fun processNull(): JsonElement = JsonNull
}

// Replace previous deepMerge with safe version (avoid 'this' shadowing inside builders)
fun JsonElement.deepMerge(other: JsonElement): JsonElement {
    if (this is JsonObject && other is JsonObject) {
        val left = this
        val right = other
        return buildJsonObject {
            // Left keys
            left.forEach { (k, v) ->
                val rv = right[k]
                if (rv != null) {
                    put(k, v.deepMerge(rv))
                } else {
                    put(k, v)
                }
            }
            // Right-only keys
            right.forEach { (k, v) ->
                if (!left.containsKey(k)) put(k, v)
            }
        }
    }
    if (this is JsonArray && other is JsonArray) {
        return buildJsonArray {
            this@deepMerge.forEach { add(it) }
            other.forEach { add(it) }
        }
    }
    // Scalars or mismatched types: overwrite with other
    return other
}
