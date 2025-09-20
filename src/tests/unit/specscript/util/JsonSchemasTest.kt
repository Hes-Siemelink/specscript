package specscript.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import specscript.TestPaths
import specscript.language.CommandFormatException
import java.nio.file.Path

class JsonSchemasTest {

    @Test
    fun `Validate Replace (Jackson)`() {
        val yaml = Yaml.readFile(example("Replace.yaml"))

        yaml.validateWithSchema("core/data-manipulation/Replace.schema")
    }

    @Test
    fun `Validate Equals with conditions schema (Jackson)`() {
        val yaml = Yaml.readFile(example("Equals.yaml"))

        yaml.validateWithSchema("core/Conditions.schema")
    }

    @Test
    fun `Validate incorrect Equals with conditions schema (Jackson)`() {
        val yaml = Yaml.readFile(example("Equals-incorrect.yaml"))

        assertThrows<CommandFormatException> {
            yaml.validateWithSchema("core/Conditions.schema")
        }
    }

    @Test
    fun `Validate nested conditions (Jackson)`() {
        val yaml = Yaml.readFile(example("Nested conditions.yaml"))

        yaml.validateWithSchema("core/Conditions.schema")
    }

    @Test
    fun `Validate nested conditions that are incorrect (Jackson)`() {
        val yaml = Yaml.readFile(example("Nested conditions-incorrect.yaml"))

        assertThrows<CommandFormatException> {
            yaml.validateWithSchema("core/Conditions.schema")
        }.let { print(it) }
    }

    @Test
    fun `Validate with Assert that schema (Jackson)`() {
        val yaml = Yaml.readFile(example("Nested conditions.yaml"))

        yaml.validateWithSchema("core/Conditions.schema")
    }

    @Test
    fun `Validate incorrect data with Assert that schema (Jackson)`() {
        val yaml = Yaml.readFile(example("Nested conditions-incorrect.yaml"))

        assertThrows<CommandFormatException> {
            yaml.validateWithSchema("core/Conditions.schema")
        }.let { print(it.toString().replace(',', '\n')) }
    }
}

//
// Helper methods
//

fun example(file: String): Path {
    return TestPaths.RESOURCES.resolve("schema/example/$file")
}
