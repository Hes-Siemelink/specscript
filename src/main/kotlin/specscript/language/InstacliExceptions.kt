package specscript.language

import com.fasterxml.jackson.databind.JsonNode
import specscript.commands.errors.ErrorData
import specscript.language.types.ObjectDefinition

open class SpecScriptException(
    message: String,
    var data: JsonNode? = null,
    cause: Throwable? = null,
    var context: String? = null
) :
    Exception(message, cause)

class CommandFormatException(message: String) : SpecScriptException(message)

class ScriptingException(message: String, data: JsonNode? = null) : SpecScriptException(message, data)

class MissingParameterException(
    message: String,
    val name: String,
    val parameters: ObjectDefinition
) :
    SpecScriptException(message)

class SpecScriptImplementationException(message: String, data: JsonNode? = null, cause: Throwable? = null) :
    SpecScriptException(message, data, cause)

open class SpecScriptCommandError(message: String, val error: ErrorData = ErrorData(message = message)) :
    Exception(message) {

    constructor(error: ErrorData) :
            this(error.message, error)

    constructor(type: String, message: String, data: JsonNode? = null) :
            this(ErrorData(type, message, data))
}
