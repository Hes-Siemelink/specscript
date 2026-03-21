package specscript.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.StringNode

class VariablesTest {

    val variables = mapOf(
        "1" to StringNode("one"),
        "2" to StringNode("two")
    )

    @Test
    fun replaceInText() {
        assertEquals("One", resolveVariablesInText("One", variables))
        assertEquals("There is one", resolveVariablesInText("There is \${1}", variables))
        assertEquals("There is one and one", resolveVariablesInText("There is \${1} and \${1}", variables))
        assertEquals(
            "There is one and one and two",
            resolveVariablesInText("There is \${1} and \${1} and \${2}", variables)
        )
    }

    @Test
    fun missingVariable() {
        assertThrows<SpecScriptException> {
            resolveVariablesInText("There is no \${3}", variables)
        }
    }

    @Test
    fun testSplit() {
        assertEquals(
            VariableWithPath("hello", null),
            splitIntoVariableAndPath("hello")
        )
        assertEquals(
            VariableWithPath("hello", ".one"),
            splitIntoVariableAndPath("hello.one")
        )
        assertEquals(
            VariableWithPath("hello", "[0]"),
            splitIntoVariableAndPath("hello[0]")
        )
        assertEquals(
            VariableWithPath("hello", "[0].one"),
            splitIntoVariableAndPath("hello[0].one")
        )
        assertEquals(
            VariableWithPath("hello", "[0].one.two"),
            splitIntoVariableAndPath("hello[0].one.two")
        )
    }

    @Test
    fun fromSimpleJsonPathToJsonPointer() {
        assertEquals(
            "/0",
            toJsonPointer("[0]").toString()
        )
        assertEquals(
            "/one",
            toJsonPointer(".one").toString()
        )
        assertEquals(
            "/a/0/b",
            toJsonPointer(".a[0].b").toString()
        )
    }

    @Test
    fun bracketNotationInJsonPointer() {
        assertEquals("/Summary.Name", toJsonPointer("[\"Summary.Name\"]").toString())
        assertEquals("/server.host/name", toJsonPointer("[\"server.host\"].name").toString())
        assertEquals("/a/0/name.first", toJsonPointer(".a[0][\"name.first\"]").toString())
        assertEquals("/first name", toJsonPointer("[\"first name\"]").toString())
    }
}