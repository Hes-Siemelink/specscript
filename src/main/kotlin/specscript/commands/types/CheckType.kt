package specscript.commands.schema

import specscript.language.*
import specscript.language.types.*
import specscript.util.toArrayNode
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

object CheckType : CommandHandler("Check type", "core/schema"), ObjectHandler {

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
        val validationErrors = messages.map { StringNode(it) }.toArrayNode()

        throw SpecScriptCommandError("Type validation errors", type = "Type validation", data = validationErrors)
    }
}

private fun getType(typeData: JsonNode, context: ScriptContext): Type {

    val type = typeData.toDomainObject(TypeSpecification::class)

    return type.resolve(context.types)
}
