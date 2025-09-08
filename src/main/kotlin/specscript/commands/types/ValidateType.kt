package specscript.commands.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import specscript.language.*
import specscript.language.types.Type
import specscript.language.types.TypeRegistry
import specscript.language.types.TypeSpecification
import specscript.language.types.resolve
import specscript.language.types.validate
import specscript.util.*

object ValidateType : CommandHandler("Validate type", "core/schema"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val json = data.getParameter("item")
        val typeData = data.getParameter("type")
        val type = getType(typeData, context)

        // Type validation throws exception when invalid
        validate(json, type, context.types)

        return null
    }
}

private fun validate(data: JsonNode, type: Type, registry: TypeRegistry) {

    val messages = type.definition.resolve(registry).definition.validate(data)

    if (messages.isNotEmpty()) {
        val validationErrors = messages.map { TextNode(it) }.toJson()

        throw SpecScriptCommandError("Type validation", "Type validation errors", validationErrors)
    }
}

private fun getType(typeData: JsonNode, context: ScriptContext): Type {

    val type = typeData.toDomainObject(TypeSpecification::class)

    return type.resolve(context.types)
}
