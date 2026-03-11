package specscript.commands.scriptinfo

import com.fasterxml.jackson.annotation.JsonAnyGetter
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import specscript.language.*
import specscript.language.types.ObjectDefinition
import specscript.language.types.ParameterData
import specscript.language.types.PropertyDefinition
import specscript.util.toDomainObject

object InputSchema : CommandHandler("Input schema", "core/script-info"),
    ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {
        val schemaData = toInputData(data)

        InputParameters.populateInputVariables(context, schemaData)

        return context.getInputVariables()
    }

    fun toInputData(data: ObjectNode): InputSchemaData {
        val propertiesNode = data.get("properties") as? ObjectNode ?: return InputSchemaData()

        val parameters = mutableMapOf<String, ParameterData>()
        for ((name, propNode) in propertiesNode.properties()) {
            parameters[name] = propNode.toDomainObject(ParameterData::class)
        }

        return InputSchemaData(parameters)
    }
}

data class InputSchemaData(
    @get:JsonAnyGetter
    override val properties: Map<String, ParameterData> = mutableMapOf()
) : ObjectDefinition
