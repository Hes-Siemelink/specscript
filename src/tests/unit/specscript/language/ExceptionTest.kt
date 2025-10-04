package specscript.language

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import specscript.TestPaths
import specscript.cli.SpecScriptCli

class ExceptionTest {

    @Test
    fun `Command format exception`() {

        // Given - Use direct file path instead of directory navigation
        val session = SpecScriptCli(
            "exceptions/command-format-exception.spec.yaml",
            workingDir = TestPaths.RESOURCES
        )

        // Then
        shouldThrow<CommandFormatException> {

            // When
            session.run()
        }
    }
}