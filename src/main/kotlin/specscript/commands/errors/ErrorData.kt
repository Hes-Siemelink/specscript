package specscript.commands.errors

import tools.jackson.databind.JsonNode

data class ErrorData(
    var type: String = "error",
    var message: String = "An error occurred",
    var data: JsonNode? = null
)
