package specscript.cli

import specscript.files.CliFileContext
import specscript.language.MissingParameterException
import specscript.language.ScriptContext
import specscript.language.SpecScriptCommandError
import specscript.language.SpecScriptException
import specscript.language.types.toDisplayString
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class CliInvocationException(message: String) : Exception(message)

fun main(args: Array<String>) {
    SpecScriptMain.main(args)
}

class SpecScriptMain(
    private val options: CliCommandLineOptions,
    private val workingDir: Path = Path.of("."),
    private val output: ConsoleOutput = StandardOutput
) {

    constructor(
        vararg args: String,
        workingDir: Path = Path.of("."),
        output: ConsoleOutput = StandardOutput
    ) : this(CliCommandLineOptions(args.toList()), workingDir, output)

    fun run(parent: ScriptContext? = null) {
        // Show usage when no commands are given (library compatibility)
        if (options.commands.isEmpty()) {
            output.printUsage(CliCommandLineOptions.definedOptions)
            return
        }

        // Resolve file using shared utility
        val filename = options.commands[0]
        val resolvedFile = try {
            CliFileUtils.resolveFile(filename, workingDir)
        } catch (e: CliInvocationException) {
            throw CliInvocationException("Could not find file: $filename")
        }

        // Create context for execution
        val context = if (parent == null) {
            CliFileContext(resolvedFile, interactive = false, workingDir = workingDir)
        } else {
            CliFileContext(resolvedFile, parent)
        }

        // Handle both files and directories (non-interactively)
        if (resolvedFile.isDirectory()) {
            invokeDirectory(resolvedFile, options.commands.drop(1), context, options)
        } else {
            // Execute using shared utility
            CliFileUtils.executeFile(resolvedFile, options, context, output)
        }
    }

    private fun invokeDirectory(
        cliDir: Path,
        args: List<String>,
        context: CliFileContext,
        options: CliCommandLineOptions
    ) {
        // Parse command
        val rawCommand = getCommand(args, context) ?: return

        // Run script
        val script = context.getCliScriptFile(rawCommand)
        if (script != null) {
            CliFileUtils.executeFile(script.file, options, CliFileContext(script.file, context), output)
            return
        }

        // Run subcommand
        val subcommand = context.getSubcommand(rawCommand)
        if (subcommand != null) {
            invokeDirectory(subcommand.dir, args.drop(1), CliFileContext(subcommand.dir, context), options)
            return
        }

        // Command not found
        throw CliInvocationException("Command '$rawCommand' not found in ${cliDir.name}")
    }

    private fun getCommand(args: List<String>, context: CliFileContext): String? {
        // Return the command if specified
        if (args.isNotEmpty()) {
            return args[0]
        }

        // Print info non-interactively
        output.printDirectoryInfo(context.info)

        // Print available commands and exit (non-interactive)
        val commands = context.getAllCommands().filter { !it.hidden }
        output.printCommands(commands)
        return null
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
                SpecScriptMain(options, workingDir = workingDir).run()

            } catch (e: CliInvocationException) {
                CliErrorReporter.reportInvocationError(e)
                return 1

            } catch (e: MissingParameterException) {
                System.err.println("Missing parameter: --${e.name}")
                System.err.println("\nOptions:")
                System.err.println(e.parameters.toDisplayString())
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