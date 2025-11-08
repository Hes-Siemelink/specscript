package specscript.test

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.function.Executable
import specscript.cli.reportError
import specscript.commands.testing.CodeExample
import specscript.commands.testing.TestCase
import specscript.commands.userinteraction.TestPrompt
import specscript.commands.userinteraction.UserPrompt
import specscript.files.*
import specscript.language.*
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

    println(Yaml.mapper.writeValueAsString(report))
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

    UserPrompt.default = TestPrompt

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
        try {
            script.run(context)
        } catch (a: Break) {
            a.output
        } catch (e: SpecScriptCommandError) {
            e.error.data?.let {
                System.err.println(it.toDisplayYaml())
            }
            throw e
        } catch (e: SpecScriptException) {
            e.reportError(printStackTrace = false)
            throw e
        }
    }
}

/**
 * Extracts the test cases from a script file as individual tests.
 */
fun SpecScriptFile.getTestCases(): List<DynamicTest> {
    val context = FileContext(file)

    return script.splitTestCases().map { script ->
        dynamicTest(script.getTestTitle(TestCase), file.toUri(), TestCaseRunner(context, script))
    }
}

/**
 * Extracts the yaml code from Markdown sections as individual tests.
 */
fun SpecScriptFile.getCodeExamplesAsTests(): List<DynamicTest> {

    // Set up test dir
    val testDir = Files.createTempDirectory("specscript-")
    testDir.toFile().deleteOnExit()
    val context = FileContext(testDir)
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
        return title!!
    }
    val command = commands.find {
        it.name == commandHandler.name
    }
    return command?.data?.textValue() ?: commandHandler.name
}
