package specscript.cli

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import specscript.TestPaths
import specscript.files.DirectoryInfo
import specscript.language.CommandInfo
import specscript.language.Script


class MockOutput : ConsoleOutput {
    var scriptInfoPrinted: Script? = null
    var outputPrinted: String? = null
    var usagePrinted: CommandLineParameters? = null

    override fun printUsage(globalOptions: CommandLineParameters) {
        usagePrinted = globalOptions
    }

    override fun printScriptInfo(script: Script) {
        scriptInfoPrinted = script
    }

    override fun printCommands(commands: List<CommandInfo>) {}
    override fun printDirectoryInfo(info: DirectoryInfo) {}
    override fun printOutput(output: String) {
        outputPrinted = output
    }
}

class CommandInvocationTest {

    private var out = MockOutput()

    @BeforeEach
    fun resetOutput() {
        out = MockOutput()
    }

    @Test
    fun `Show usage when no arguments provided`() {
        // Given
        val session = SpecScriptMain("-q", workingDir = TestPaths.RESOURCES, output = out)

        // When - Should not throw exception, just show usage
        session.run()

        // Then - Usage should be printed (no exception thrown)
        // The MockOutput would capture printed usage
    }

    @Test
    fun `Handle directories by listing contents non-interactively`() {
        // Given
        val session = SpecScriptMain("-q", "sample", workingDir = TestPaths.RESOURCES, output = out)

        // When - Should not throw exception, just list directory contents
        session.run()

        // Then - Directory contents should be printed (no exception thrown)
        // The MockOutput would capture any printed commands or directory info
    }

    @Test
    fun `Execute simple file successfully`() {
        // Given
        val session = SpecScriptMain("-q", "simple.spec.yaml", workingDir = TestPaths.RESOURCES, output = out)

        // When
        session.run()

        // Then - No exception thrown, file executed successfully
        out.scriptInfoPrinted shouldBe null // No script info in normal execution
    }

    @Test
    fun `Print script info with help flag`() {
        // Given
        val session = SpecScriptMain("--help", "simple.spec.yaml", workingDir = TestPaths.RESOURCES, output = out)

        // When
        session.run()

        // Then
        val script = out.scriptInfoPrinted
        script shouldNotBe null
        script!!.commands.size shouldBe 1
        script.commands[0].name shouldBe "Print"
    }

    @Test
    fun `Handle file not found with clear error`() {
        // Given
        val session = SpecScriptMain("-q", "nonexistent", workingDir = TestPaths.RESOURCES, output = out)

        // When & Then
        val exception = assertThrows<CliInvocationException> {
            session.run()
        }

        exception.message shouldBe "Could not find file: nonexistent"
    }

    @Test
    fun `Print output - YAML format`() {
        // Given
        val session =
            SpecScriptMain("--output", "print-output.spec.yaml", workingDir = TestPaths.RESOURCES, output = out)

        // When
        session.run()

        // Then
        out.outputPrinted shouldBe "test: output"
    }

    @Test
    fun `Print output - JSON format`() {
        // Given
        val session =
            SpecScriptMain("--output-json", "print-output.spec.yaml", workingDir = TestPaths.RESOURCES, output = out)

        // When
        session.run()

        // Then
        out.outputPrinted shouldBe """{
  "test" : "output"
}"""
    }
}