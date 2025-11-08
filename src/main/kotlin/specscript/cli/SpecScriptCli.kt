package specscript.cli

import com.fasterxml.jackson.databind.JsonNode
import specscript.files.FileContext
import specscript.files.SpecScriptFile
import specscript.language.*
import specscript.language.types.toDisplayString
import specscript.test.runTests
import specscript.util.add
import specscript.util.toDisplayJson
import specscript.util.toDisplayYaml
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class CliInvocationException(message: String) : Exception(message)

fun main(args: Array<String>) {
    SpecScriptCli.main(args)
}

class SpecScriptCli(
    private val options: CliCommandLineOptions,
    private val workingDir: Path = Path.of("."),
    private val input: UserInput = StandardInput,
    private val output: ConsoleOutput = StandardOutput
) {

    constructor(
        vararg args: String,
        workingDir: Path = Path.of("."),
        input: UserInput = StandardInput,
        output: ConsoleOutput = StandardOutput
    ) : this(CliCommandLineOptions(args.toList()), workingDir, input, output)

    fun run(parent: ScriptContext? = null) {

        // Show usage when no commands are given
        if (options.commands.isEmpty()) {
            output.printUsage(CliCommandLineOptions.definedOptions)
            return
        }

        // Find command file
        val command = options.commands[0]
        val resolvedFile = resolveCommand(command, workingDir)

        // Run in test mode
        if (options.testMode) {
            runTests(resolvedFile)
            return
        }

        // Create context
        val context = if (parent == null) {
            FileContext(resolvedFile, interactive = options.interactive, workingDir = workingDir)
        } else {
            FileContext(resolvedFile, parent)
        }

        // Handle file or directory
        if (resolvedFile.isDirectory()) {
            invokeDirectory(resolvedFile, options.commands.drop(1), context, options)
        } else {
            executeFile(resolvedFile, options, context, output)
        }
    }


    private fun invokeDirectory(
        cliDir: Path,
        args: List<String>,
        context: FileContext,
        options: CliCommandLineOptions
    ) {
        val rawCommand = getCommand(args, context) ?: return

        // Run script
        val script = context.getCliScriptFile(rawCommand)
        if (script != null) {
            executeFile(script.file, options, FileContext(script.file, context), output)
            return
        }

        // Recursively run subcommand in subdirectory
        val subcommand = context.getSubcommand(rawCommand)
        if (subcommand != null) {
            invokeDirectory(subcommand.dir, args.drop(1), FileContext(subcommand.dir, context), options)
            return
        }

        // Command not found
        throw CliInvocationException("Command '$rawCommand' not found in ${cliDir.name}")
    }

    private fun getCommand(args: List<String>, context: FileContext): String? {

        // Return the command if specified
        if (args.isNotEmpty()) {
            return args[0]
        }


        // Print info
        output.printDirectoryInfo(context.info)

        // Select command
        val commands = context.getAllCommands().filter { !it.hidden }
        return when {
            context.interactive && !options.help -> input.askForCommand(commands)
            else -> {
                output.printCommands(commands)
                null
            }
        }
    }

    companion object {
        fun main(args: Array<String>, workingDir: Path = Path.of(".")): Int {
            val options = try {
                CliCommandLineOptions(args.toList())
            } catch (e: CliInvocationException) {
                CliErrorReporter.reportInvocationError(e)
                return 1
            }

            try {
                SpecScriptCli(options, workingDir = workingDir).run()

            } catch (e: CliInvocationException) {
                CliErrorReporter.reportInvocationError(e)
                return 1

            } catch (e: MissingInputException) {
                System.err.println("Missing parameter: --${e.name}")
                System.err.println("\nOptions:")
                System.err.println(e.info.toDisplayString())
                return 1

            } catch (e: SpecScriptException) {
                CliErrorReporter.reportLanguageError(e, options.debug)
                return 1

            } catch (e: SpecScriptCommandError) {
                CliErrorReporter.reportCommandError(e)
                return 1
            }

            return 0
        }
    }
}

fun resolveCommand(command: String, workingDir: Path): Path {
    // Try exact filename first
    workingDir.resolve(command).let {
        if (it.exists()) {
            return it
        }
    }

    // Try with .spec.yaml extension
    workingDir.resolve("$command.spec.yaml").let {
        if (it.exists()) {
            return it
        }
    }

    // Try with .spec.md extension
    workingDir.resolve("$command.spec.md").let {
        if (it.exists()) {
            return it
        }
    }

    throw CliInvocationException("Could not find spec file for: $command")
}

fun executeFile(
    file: Path,
    options: CliCommandLineOptions,
    context: FileContext,
    output: ConsoleOutput
): JsonNode? {
    val scriptFile = SpecScriptFile(file)

    // Handle help request
    if (options.help) {
        output.printScriptInfo(scriptFile.script)
        return null
    }

    // Set up parameters
    context.getInputVariables().add(options.commandParameters)

    // Execute script
    val result = scriptFile.script.run(context)

    // Handle output based on options
    when (options.printOutput) {
        OutputOption.YAML -> output.printOutput(result.toDisplayYaml())
        OutputOption.JSON -> output.printOutput(result.toDisplayJson())
        else -> {} // No output
    }

    return result
}
