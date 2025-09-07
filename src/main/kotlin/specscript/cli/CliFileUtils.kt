package specscript.cli

import com.fasterxml.jackson.databind.JsonNode
import specscript.files.CliFile
import specscript.files.CliFileContext
import specscript.language.getInputVariables
import specscript.language.run
import specscript.util.add
import specscript.util.toDisplayYaml
import specscript.util.toDisplayJson
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Utility functions for CLI file operations.
 */
object CliFileUtils {

    /**
     * Resolves a filename to an actual file path, supporting both exact names and .cli extension.
     * 
     * @param filename The filename to resolve (with or without .cli extension)
     * @param workingDir The directory to search in
     * @return Path to the resolved file
     * @throws CliInvocationException if the file cannot be found
     */
    fun resolveFile(filename: String, workingDir: Path): Path {
        // Try exact filename first
        workingDir.resolve(filename).let {
            if (it.exists()) {
                return it
            }
        }

        // Try with .cli extension
        workingDir.resolve("$filename.cli").let {
            if (it.exists()) {
                return it
            }
        }

        throw CliInvocationException("Could not find command: $filename")
    }

    /**
     * Validates that a file exists and is accessible.
     * 
     * @param file The file path to validate
     * @return The same path if valid
     * @throws CliInvocationException if the file doesn't exist or is not accessible
     */
    fun validateFileExists(file: Path): Path {
        if (!file.exists()) {
            throw CliInvocationException("File does not exist: $file")
        }
        return file
    }

    /**
     * Executes a CLI file with the given options and context.
     * 
     * This handles the core execution logic including:
     * - Setting up command parameters
     * - Running the script
     * - Processing output based on options
     * 
     * @param file The file to execute
     * @param options CLI options including output format
     * @param context The execution context
     * @param output Console output handler
     * @return The script execution result
     */
    fun executeFile(
        file: Path, 
        options: CliCommandLineOptions, 
        context: CliFileContext,
        output: ConsoleOutput
    ): JsonNode? {
        val cliFile = CliFile(file)

        // Handle help request
        if (options.help) {
            output.printScriptInfo(cliFile.script)
            return null
        }

        // Set up parameters
        context.getInputVariables().add(options.commandParameters)

        // Execute script
        val result = cliFile.script.run(context)

        // Handle output based on options
        when (options.printOutput) {
            OutputOption.YAML -> output.printOutput(result.toDisplayYaml())
            OutputOption.JSON -> output.printOutput(result.toDisplayJson())
            else -> {} // No output
        }

        return result
    }
}