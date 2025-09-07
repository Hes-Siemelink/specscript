package specscript.cli

import com.fasterxml.jackson.databind.JsonNode
import specscript.language.InstacliCommandError
import specscript.language.InstacliLanguageException
import specscript.util.Json
import specscript.util.toDisplayYaml

/**
 * Utility class for consistent error reporting.
 * 
 * Provides standardized error formatting and output handling.
 */
object CliErrorReporter {

    /**
     * Reports an InstacliLanguageException with optional stack trace.
     * 
     * @param exception The language exception to report
     * @param printStackTrace Whether to include stack traces in debug mode
     */
    fun reportLanguageError(exception: InstacliLanguageException, printStackTrace: Boolean) {
        System.err.println("\nInstacli scripting error")

        // Exception caused by incorrect instacli script
        if (exception.cause == null || exception.cause is InstacliLanguageException) {
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

        // Print Instacli context
        exception.data?.let {
            val yaml = exception.data.toDisplayYaml().prependIndent("  ")
            val message = "In ${exception.context ?: "command"}:"
            System.err.println("\n\n$message\n\n${yaml}".trimMargin())
        }
    }

    fun reportCommandError(commandError: InstacliCommandError) {
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

fun InstacliLanguageException.reportError(printStackTrace: Boolean) {
    CliErrorReporter.reportLanguageError(this, printStackTrace)
}

fun InstacliCommandError.reportError() {
    CliErrorReporter.reportCommandError(this)
}