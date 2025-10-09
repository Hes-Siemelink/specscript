package specscript.cli

import com.fasterxml.jackson.databind.JsonNode
import specscript.language.SpecScriptCommandError
import specscript.language.SpecScriptException
import specscript.util.Json
import specscript.util.toDisplayYaml

/**
 * Utility class for consistent error reporting.
 *
 * Provides standardized error formatting and output handling.
 */
object CliErrorReporter {

    /**
     * Reports a SpecScriptLanguageException with optional stack trace.
     *
     * @param exception The language exception to report
     * @param printStackTrace Whether to include stack traces in debug mode
     */
    fun reportLanguageError(exception: SpecScriptException, printStackTrace: Boolean) {
        System.err.println("\nScripting error")

        // Exception caused by incorrect SpecScript script
        if (exception.cause == null || exception.cause is SpecScriptException) {
            System.err.println("\n${exception.message}")
        } else {
            // Unexpected exception from command handler implementation
            if (printStackTrace) {
                System.err.print("\nCaused by: ")
                exception.cause?.printStackTrace()
            } else {
                System.err.println("\nCaused by: ${exception.cause}")
            }
        }

        // Print SpecScript context
        exception.command?.let {
            val yaml = exception.command.toDisplayYaml().prependIndent("  ")
            val message = "In ${exception.context ?: "command"}:"
            System.err.println("\n\n$message\n\n${yaml}".trimMargin())
        }
    }

    fun reportCommandError(commandError: SpecScriptCommandError) {
        System.err.println(commandError.message)
        if (commandError.message != commandError.error.message) {
            System.err.println(commandError.error.message)
        }

        if (commandError.error.data != null) {
            val details = Json.newObject()
            details.set<JsonNode>(commandError.error.type, commandError.error.data)
            System.err.println(details.toDisplayYaml())
        }
    }

    fun reportInvocationError(exception: CliInvocationException) {
        System.err.println(exception.message)
    }
}

fun SpecScriptException.reportError(printStackTrace: Boolean) {
    CliErrorReporter.reportLanguageError(this, printStackTrace)
}
