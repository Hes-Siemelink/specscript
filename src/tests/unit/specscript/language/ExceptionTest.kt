package specscript.language

import specscript.TestPaths
import specscript.cli.InstacliMain
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class ExceptionTest {

    @Test
    fun `Command format exception`() {

        // Given
        val session = InstacliMain(
            "-q", "exceptions", "command-format-exception.cli",
            workingDir = TestPaths.RESOURCES
        )

        // Then
        shouldThrow<CommandFormatException> {

            // When
            session.run()
        }
    }
}

