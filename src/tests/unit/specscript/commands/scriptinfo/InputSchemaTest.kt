package specscript.commands.scriptinfo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import specscript.language.CommandFormatException
import specscript.util.Yaml
import tools.jackson.databind.node.ObjectNode

class InputSchemaTest {

    private fun inputSchema(yaml: String) =
        InputSchema.toInputData(Yaml.parse(yaml) as ObjectNode)

    @Test
    fun `valid default property is exposed`() {
        val data = inputSchema(
            """
            type: [ object, string ]
            x-default-property: name
            properties:
              name:
                type: string
            """.trimIndent()
        )

        data.defaultProperty shouldBe "name"
    }

    @Test
    fun `missing default property fails at load`() {
        shouldThrow<CommandFormatException> {
            inputSchema(
                """
                x-default-property: missing
                properties:
                  name:
                    type: string
                """.trimIndent()
            )
        }
    }

    @Test
    fun `default property naming a non-scalar property fails at load`() {
        shouldThrow<CommandFormatException> {
            inputSchema(
                """
                x-default-property: config
                properties:
                  config:
                    type: object
                """.trimIndent()
            )
        }
    }
}
