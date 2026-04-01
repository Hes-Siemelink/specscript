package specscript.language

import specscript.commands.files.TempFile
import specscript.commands.files.TempFileData
import specscript.commands.scriptinfo.*
import specscript.commands.shell.Cli
import specscript.commands.shell.CliData
import specscript.commands.shell.Shell
import specscript.commands.shell.ShellCommand
import specscript.commands.testing.*
import specscript.commands.testing.Answers
import specscript.commands.util.Print
import specscript.files.MarkdownBlock
import specscript.files.MarkdownBlock.*
import specscript.util.Yaml
import specscript.util.toDomainObject
import specscript.util.toJsonNode
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode
import kotlin.io.path.name

data class Command(val name: String, val data: JsonNode)

class Break(val output: JsonNode) : Exception()

class Script(val commands: List<Command>, val title: String? = null) {

    val info: ScriptInfoData by lazy {
        getScriptInfo()
    }

    fun run(context: ScriptContext): JsonNode? {
        return try {
            ExpectedConsoleOutput.captureSystemOutAndErr(context) {
                runCommands(context)
            }
        } catch (a: Break) {
            a.output
        } catch (e: SpecScriptException) {
            e.context = e.context ?: context.scriptFile.name
            throw e
        }
    }

    fun runCommands(context: ScriptContext): JsonNode? {
        var output: JsonNode? = null

        for (command in commands) {
            val handler = context.getCommandHandler(command.name)

            if (context.error != null && handler !is ErrorHandler) {
                continue
            }

            try {
                output = runCommand(handler, command.data, context) ?: output
            } catch (e: SpecScriptCommandError) {
                context.error = e
            }
        }

        context.error?.let { throw it }

        return output
    }

    private fun getScriptInfo(): ScriptInfoData {
        val scriptInfoCommand = commands.find { it.equalsCommand(ScriptInfo) }
        val scriptInfoData = scriptInfoCommand?.data?.toDomainObject(ScriptInfoData::class) ?: ScriptInfoData(title)

        val inputParameterCommand = commands.find { it.equalsCommand(InputParameters) }
        val inputSchemaCommand = commands.find { it.equalsCommand(InputSchema) }

        return when {
            inputParameterCommand != null -> {
                val inputParams = inputParameterCommand.data.toDomainObject(InputParameterData::class)
                val mergedInput = (scriptInfoData.input ?: emptyMap()) + (inputParams.properties)
                scriptInfoData.copy(input = mergedInput)
            }

            inputSchemaCommand != null -> {
                val schemaData =
                    InputSchema.toInputData(inputSchemaCommand.data as tools.jackson.databind.node.ObjectNode)
                val mergedInput = (scriptInfoData.input ?: emptyMap()) + (schemaData.properties)
                scriptInfoData.copy(input = mergedInput)
            }

            else -> scriptInfoData
        }
    }

    companion object {
        fun from(script: List<JsonNode>): Script {
            return Script(toCommandList(script))
        }

        fun from(data: JsonNode): Script {
            return Script(toCommandList(data))
        }

        fun from(content: String): Script {
            return Script(toCommandList(content))
        }
    }
}

private fun toCommandList(script: List<JsonNode>): List<Command> {
    return script.map { scriptNode -> toCommandList(scriptNode) }.flatten()
}

private fun toCommandList(scriptNode: JsonNode): List<Command> {
    if (scriptNode.isArray) {
        return scriptNode.flatMap { element -> toCommandList(element) }
    }
    return scriptNode.properties().asSequence().map { Command(it.key, it.value) }.toList()
}

private fun toCommandList(script: String): List<Command> {
    return toCommandList(Yaml.parseAsFile(script))
}

fun JsonNode.run(context: ScriptContext): JsonNode? {
    return Script.from(this).runCommands(context)
}

fun List<MarkdownBlock>.toScript(): Script {

    val commands = mutableListOf<Command>()
    var title: String? = null
    for (block in this) {
        when (block.type) {
            Header -> {
                title = block.headerLine.substring(block.headerLine.indexOf(' ')).trim()
            }

            SpecScriptYaml, HiddenSpecScriptYaml -> {
                commands.addAll(toCommandList(block.getContent()))
            }

            YamlFile -> {
                val data = TempFileData(
                    filename = block.getOption("temp-file"),
                    content = StringNode(block.getContent()),
                    resolve = block.getOption("resolve")?.toBoolean() ?: false,
                )
                commands.add(
                    Command(TempFile.name, data.toJsonNode())
                )
            }

            ShellCli -> {
                if (block.headerLine.contains("ignore")) continue

                val data = CliData(
                    command = block.getContent(),
                    cd = block.getOption("cd")
                )
                commands.add(
                    Command(Cli.name, data.toJsonNode())
                )
            }

            ShellBlock -> {
                if (block.headerLine.contains("ignore")) continue

                val explicitCd = block.getOption("cd")
                val cd = when {
                    explicitCd == null -> "\${SCRIPT_HOME}"
                    explicitCd.startsWith("/") || explicitCd.startsWith("\${") -> explicitCd
                    else -> "\${SCRIPT_HOME}/$explicitCd"
                }

                val data = ShellCommand(
                    command = block.getContent(),
                    showOutput = block.getOption("show_output")?.toBoolean() ?: true,
                    showCommand = block.getOption("show_command")?.toBoolean() ?: false,
                    cd = cd
                )
                commands.add(
                    Command(Shell.name, data.toJsonNode())
                )
            }

            MarkdownBlock.Answers -> {
                commands.add(
                    Command(Answers.name, Yaml.parse(block.getContent()))
                )
            }

            Quote -> {
                commands.add(
                    Command(Print.name, StringNode(block.getContent()))
                )
            }

            Output -> {
                commands.add(
                    Command(ExpectedConsoleOutput.name, StringNode(block.getContent()))
                )
            }
        }
    }

    return Script(commands, title)
}

/**
 * Gets all test cases as a separate script
 */
fun Script.splitTestCases(): List<Script> {

    val allTests = mutableListOf<Script>()

    var currentCase = mutableListOf<Command>()
    var testCaseFound = false
    for (command in commands) {
        if (command.equalsCommand(TestCase)) {
            if (!testCaseFound) {
                // Ignore everything before the first 'Test case' command
                testCaseFound = true
            } else {
                // Add everything that was recorded since the 'Test case' command
                allTests.add(Script(currentCase))
            }
            currentCase = mutableListOf()
        }

        currentCase.add(command)
    }

    // Add the last test case
    if (testCaseFound) {
        allTests.add(Script(currentCase))
    }

    return allTests
}

data class NamedTest(val name: String, val script: Script)

class TestSuite(
    val setup: Script?,
    val tests: List<NamedTest>,
    val teardown: Script?
)

fun Script.splitTests(): TestSuite {
    var setup: Script? = null
    val tests = mutableListOf<NamedTest>()
    var teardown: Script? = null

    for (command in commands) {
        when {
            command.equalsCommand(BeforeTests) -> {
                setup = Script.from(command.data)
            }

            command.equalsCommand(AfterTests) -> {
                teardown = Script.from(command.data)
            }

            command.equalsCommand(Tests) -> {
                for (field in command.data.properties()) {
                    tests.add(NamedTest(field.key, Script.from(field.value)))
                }
            }
        }
    }

    return TestSuite(setup, tests, teardown)
}
