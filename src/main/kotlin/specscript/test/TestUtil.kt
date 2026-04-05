package specscript.test

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.function.Executable
import specscript.commands.testing.CodeExample
import specscript.commands.testing.TestCase
import specscript.commands.testing.Tests
import specscript.files.*
import specscript.language.*
import specscript.util.IO
import specscript.util.Yaml
import specscript.util.toDisplayYaml
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name


//
// Run tests
//

fun runTests(file: Path) {
    val report = TestReport()
    runTestSuite(file.getTests(), report)

    println(Yaml.writeAsString(report))
}

fun runTestSuite(nodes: List<DynamicNode>, report: TestReport) {
    fun run(node: DynamicNode) {
        when (node) {
            is DynamicTest -> {
                try {
                    node.executable.execute()
                    report.passed += 1
                } catch (e: Throwable) {
                    report.failed += 1
                    report.details += TestFailure(node.displayName, e.message ?: "")
                }
            }

            is DynamicContainer -> node.children.forEach { run(it) }
        }
    }
    nodes.forEach { run(it) }
}

class TestReport() {
    var passed: Int = 0
    var failed: Int = 0
    var details: List<TestFailure> = emptyList()
}

class TestFailure(
    val testCase: String,
    val error: String
)

//
// Extract tests from files
//

fun Path.getTests(): List<DynamicNode> {

    if (isDirectory()) {
        return listDirectoryEntries().mapNotNull {
            val tests = it.getTests()
            if (tests.isEmpty()) {
                null
            } else {
                dynamicContainer(it.name, tests)
            }
        }
    } else {
        // TODO: Get test cases from Markdown and Code examples from Yaml
        if (name.endsWith(YAML_SPEC_EXTENSION)) {
            return SpecScriptFile(this).getTestCases()
        } else if (name.endsWith(MARKDOWN_SPEC_EXTENSION)) {
            return SpecScriptFile(this).getCodeExamplesAsTests()
        }
    }
    return emptyList()
}

class TestCaseRunner(
    val context: ScriptContext,
    val script: Script
) : Executable {

    override fun execute() {
        context.error = null
        context.variables.remove(INPUT_VARIABLE)

        var failure: Throwable? = null
        val (stdout, stderr) = IO.captureSystemOutAndErr(echo = false) {
            try {
                script.run(context)
            } catch (a: Break) {
                a.output
            } catch (e: Throwable) {
                failure = e
            }
        }

        if (failure != null) {
            if (stdout.isNotBlank()) System.out.print(stdout)
            if (stderr.isNotBlank()) System.err.print(stderr)
            throw failure!!
        }
    }
}

/**
 * Extracts the test cases from a script file as individual tests.
 * Supports both legacy Test case commands and new Tests/Before all tests/After all tests commands.
 */
fun SpecScriptFile.getTestCases(): List<DynamicTest> {
    val hasNewTests = script.commands.any { it.equalsCommand(Tests) }
    val hasLegacyTests = script.commands.any { it.equalsCommand(TestCase) }

    if (!hasNewTests && !hasLegacyTests) {
        return emptyList()
    }

    val scriptDir = file.toAbsolutePath().normalize().parent
    val context = FileContext(file, workingDir = scriptDir)

    if (hasNewTests) {
        return getTests(context)
    }

    return script.splitTestCases().map { script ->
        dynamicTest(script.getTestTitle(TestCase), file.toUri(), TestCaseRunner(context, script))
    }
}

private fun SpecScriptFile.getTests(context: ScriptContext): List<DynamicTest> {
    val suite = script.splitTests()

    return suite.tests.mapIndexed { index, namedTest ->
        val commands = mutableListOf<Command>()

        if (index == 0 && suite.setup != null) {
            commands.addAll(suite.setup.commands)
        }

        commands.addAll(namedTest.script.commands)

        if (index == suite.tests.lastIndex && suite.teardown != null) {
            commands.addAll(suite.teardown.commands)
        }

        dynamicTest(namedTest.name, file.toUri(), TestCaseRunner(context, Script(commands)))
    }
}

/**
 * Extracts the yaml code from Markdown sections as individual tests.
 *
 * Creates a temp dir that serves as both scriptDir and tempDir (so file= blocks and
 * file resolution share the same directory). The scriptHome parameter preserves the
 * real spec file location for SCRIPT_HOME.
 */
fun SpecScriptFile.getCodeExamplesAsTests(): List<DynamicTest> {

    val testDir = Files.createTempDirectory("specscript-")
    testDir.deleteOnShutdown()
    val scriptHome = file.toAbsolutePath().normalize().parent
    val context = FileContext(testDir, scriptHome = scriptHome)
    context.setTempDir(testDir)

    val scripts = splitMarkdown()
    val tests: List<DynamicTest> = scripts
        .mapNotNull {
            toTestFromScript(file, it, context)
        }

    return tests
}

private fun toTestFromScript(
    document: Path,
    script: Script,
    context: ScriptContext,
): DynamicTest? {

    // Filter out sections that don't have any commands
    if (script.commands.isEmpty()) {
        return null
    }

    val title = script.getTestTitle(CodeExample)

    return dynamicTest(title, document.toUri(), TestCaseRunner(context, script))
}

fun Script.getTestTitle(commandHandler: CommandHandler): String {
    if (title != null) {
        return title
    }
    val command = commands.find { it.equalsCommand(commandHandler) }
    return command?.data?.stringValue() ?: commandHandler.name
}
