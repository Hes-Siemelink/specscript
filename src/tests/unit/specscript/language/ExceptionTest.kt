package specscript.language

import specscript.TestPaths
import specscript.cli.SpecScriptMain
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class ExceptionTest {

    @Test
    fun `Command format exception`() {

        // Given - Use direct file path instead of directory navigation
        val session = SpecScriptMain(
            "-q", "exceptions/command-format-exception.cli",
            workingDir = TestPaths.RESOURCES
        )

        // Then
        shouldThrow<CommandFormatException> {

            // When
            session.run()
        }
    }
}