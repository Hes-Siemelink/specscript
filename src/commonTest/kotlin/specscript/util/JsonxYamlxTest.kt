package specscript.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonxYamlxTest {

    // TODO: Reinstate YAML parse tests once Yamlx supports stable JsonElement conversion.

    @Test
    fun jsonxNewObjectAndArray() {
        val obj = Jsonx.newObject(mapOf("a" to "1", "b" to "2"))
        assertEquals("1", (obj["a"] as JsonPrimitive).content)
        val arr = Jsonx.newArray()
        val mergedArr = arr + listOf(JsonPrimitive("x"))
        assertEquals(1, mergedArr.size)
    }

    @Test
    fun deepMergeObjectsAndArrays() {
        val left = Jsonx.parse("""{ "o": { "x": 1 }, "a": [1], "s": "old" }""")
        val right = Jsonx.parse("""{ "o": { "y": 2 }, "a": [2,3], "s": "new" }""")
        val merged = left.deepMerge(right) as JsonObject
        val o = merged["o"] as JsonObject
        assertEquals("1", ((o["x"] as JsonPrimitive).content))
        assertEquals("2", ((o["y"] as JsonPrimitive).content))
        val a = merged["a"] as JsonArray
        assertEquals(3, a.size) // concatenated
        assertEquals("new", (merged["s"] as JsonPrimitive).content)
    }

    @Test
    fun parseIfPossibleNull() {
        val element = Jsonx.parseIfPossible(null)
        assertEquals("", (element as JsonPrimitive).content)
    }

    @Test
    fun deepMergeScalars() {
        val a: JsonElement = JsonPrimitive("old")
        val b: JsonElement = JsonPrimitive("new")
        val merged = a.deepMerge(b)
        assertEquals("new", (merged as JsonPrimitive).content)
    }

    @Test
    fun addVarsToObject() {
        val base = Jsonx.newObject("k", "v")
        val extended = base.add(mapOf("k2" to "v2"))
        assertEquals("v", (extended["k"] as JsonPrimitive).content)
        assertEquals("v2", (extended["k2"] as JsonPrimitive).content)
    }

    @Test
    fun processorIdentity() {
        val element = Jsonx.parse("""{ "a": [1,2], "b": "c" }""")
        val processed = object : JsonxProcessor() {}.process(element)
        assertEquals(element, processed)
    }
}
