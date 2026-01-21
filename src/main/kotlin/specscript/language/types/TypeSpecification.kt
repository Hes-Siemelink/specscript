package specscript.language.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

data class TypeSpecification(
    val name: String? = null,
    val base: String? = null,
    val properties: ObjectProperties? = null,
    @JsonProperty("list of")
    val listOf: TypeSpecification? = null,
) {
    @JsonCreator
    constructor(typeName: String) : this(name = typeName)
}

fun TypeSpecification.validate(data: JsonNode): List<String> {

    return when (base) {

        Type.STRING -> {
            if (data !is StringNode) {
                return listOf("Data should be string but is ${data::class.simpleName}")
            }
            emptyList()
        }

        Type.BOOLEAN -> {
            if (data !is BooleanNode) {
                return listOf("Data should be boolean but is ${data::class.simpleName}")
            }
            emptyList()
        }

        Type.OBJECT -> {
            if (data !is ObjectNode) {
                return listOf("Data should be object but is ${data::class.simpleName}")
            }
            properties?.validate(data) ?: emptyList()
        }

        Type.ARRAY -> {
            if (data !is ArrayNode) {
                return listOf("Data should be array but is ${data::class.simpleName}")
            }
            data.flatMap { item ->
                listOf?.validate(item) ?: emptyList()
            }
        }

        null -> throw error("Type definition must have a base:\n$this")

        else -> emptyList()
    }
}
