package specscript.language

import com.fasterxml.jackson.databind.JsonNode
import specscript.commands.errors.ErrorData
import specscript.language.types.ObjectDefinition

/**
 * Base exception class for all SpecScript-related errors.
 *
 * @param message The error message
 * @param data Optional JSON data associated with the error
 * @param cause The underlying cause of the exception
 * @param context Optional context information about where the error occurred
 */
open class SpecScriptException(
    message: String,
    var data: JsonNode? = null,
    cause: Throwable? = null,
    var context: String? = null
) : Exception(message, cause)

/**
 * Indicates that a command has invalid format or structure.
 * Used for syntax errors and malformed command definitions.
 */
class CommandFormatException(message: String) : SpecScriptException(message)

/**
 * Indicates that a runtime errors occurred during script execution. when
 */
class ScriptingException(message: String, data: JsonNode? = null) : SpecScriptException(message, data)

/**
 * Indicates a missing required parameter
 *
 * @param name The name of the missing parameter
 * @param parameters The object definition containing parameter requirements
 */
class MissingParameterException(
    message: String,
    val name: String,
    val parameters: ObjectDefinition
) : SpecScriptException(message)

/**
 * Used for internal errors that indicate bugs in the SpecScript implementation.
 */
class SpecScriptImplementationException(message: String, data: JsonNode? = null, cause: Throwable? = null) :
    SpecScriptException(message, data, cause)

/**
 * Exception thrown when a command execution fails with structured error information.
 * This is the preferred exception type for command handlers to provide rich error details.
 *
 * @param error The structured error data containing type, message, and optional data
 */
open class SpecScriptCommandError(message: String, val error: ErrorData = ErrorData(message = message)) :
    Exception(message) {

    constructor(error: ErrorData) : this(error.message, error)

    constructor(type: String, message: String, data: JsonNode? = null) :
            this(ErrorData(type, message, data))
}
