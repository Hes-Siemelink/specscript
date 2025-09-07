package specscript.commands

import specscript.TestPaths
import specscript.util.JsonSchemas
import specscript.util.Yaml
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ConditionSchemaTest {

    @Test
    fun `validate equals`() {
        val condition = """
            item: a
            equals: b
        """.trimIndent()

        validate(condition, "Equals.schema.yaml")
    }

    fun validate(json: String, schemaFile: String) {

        val node = Yaml.parse(json)
        val schema = JsonSchemas.load(TestPaths.TEST_SCHEMAS.resolve(schemaFile)) ?: error("Schema not found")
        val messages = schema.validate(node)
        messages.forEach {
            println(it)
        }
        messages.size shouldBe 0
    }
}