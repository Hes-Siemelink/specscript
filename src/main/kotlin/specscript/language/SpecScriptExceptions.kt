package specscript.language

import com.fasterxml.jackson.databind.JsonNode
import specscript.commands.errors.ErrorData
import specscript.language.types.ObjectDefinition

/**
 * Base exception class for all SpecScript-related errors.
 *
 * @param message The error message
 * @param cause The underlying cause of the exception
 * @param command JSON data of the command causing the error. Will be filled in by the executor.
 */
open class SpecScriptException(
    override val message: String,
    cause: Throwable? = null,
    var command: JsonNode? = null
) : Exception(message, cause) {

    /** Context where the error occurred, for example the filename. Will be filled in by the executor. */
    var context: String? = null
}

/**
 * Indicates that a command has invalid format or structure.
 */
class CommandFormatException(message: String) : SpecScriptException(message)

/**
 * Indicates a missing required input parameter
 *
 * @param name The name of the missing parameter
 * @param info The object definition containing parameter requirements
 */
class MissingInputException(
    message: String,
    val name: String,
    val info: ObjectDefinition
) : SpecScriptException(message)

/**
 * Wraps unhandled exceptions.
 */
class SpecScriptInternalError(message: String, cause: Throwable? = null, command: JsonNode? = null) :
    SpecScriptException(message, cause, command = command)

/**
 * Exception that can be handled by the `On error` command. in SpecScript.
 */
open class SpecScriptCommandError(
    message: String,
    cause: Throwable? = null,
    val type: String = "error",
    val data: JsonNode? = null
) :
    SpecScriptException(message, cause) {

    val error: ErrorData
        get() = ErrorData(type, message, data)
}
