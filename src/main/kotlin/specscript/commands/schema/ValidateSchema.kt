package specscript.commands.schema

import com.networknt.schema.Schema
import specscript.language.*
import specscript.util.Json
import specscript.util.JsonSchemas
import specscript.util.toJson
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

object ValidateSchema : CommandHandler("Validate schema", "core/schema"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val json = data.getParameter("data")

        // Schema validation
        data.get("schema")?.let {
            val schema = it.getSchema(context)
            json.validateWithSchema(schema)
        }

        return null
    }
}


//
// Schema support
//

private fun JsonNode.validateWithSchema(schema: Schema) {

    val messages = schema.validate(this)

    if (messages.isNotEmpty()) {
        val validationErrors = messages.map {
            Json.newObject(it.messageKey, it.message)
        }.toJson()

        throw SpecScriptCommandError(
            "Schema validation error",
            type = "Schema validation error",
            data = validationErrors
        )
    }
}

private fun JsonNode.getSchema(context: ScriptContext): Schema {

    return if (this is StringNode) {
        val location = context.scriptDir.resolve(textValue())
        JsonSchemas.registry.getSchema(location.toUri().toString())
    } else {
        JsonSchemas.registry.getSchema(this)
    }
}
