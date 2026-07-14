package specscript.language.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import specscript.commands.toCondition
import tools.jackson.databind.JsonNode

abstract class PropertyDefinition {

    abstract val title: String?
    abstract val description: String?
    abstract val optional: Boolean
    abstract val default: JsonNode?
    abstract val type: TypeSpecification?
    abstract val format: String?
    abstract val enum: List<JsonNode>?
    abstract val items: PropertyDefinition?
    abstract val titleProperty: String?
    abstract val valueProperty: String?
    abstract val condition: JsonNode?
    abstract val shortOption: String?
    abstract val env: String?

    /** The question shown to the user. Falls back to the description, then the given name. */
    fun question(name: String? = null): String = title ?: description ?: name ?: ""

    val isPassword: Boolean get() = format == "password"

    val isMultiple: Boolean get() = type?.name == "array"

    fun conditionValid(): Boolean {
        condition?.let { node ->
            return node.toCondition().isTrue()
        }
        return true
    }
}

/**
 * Used in type definitions to define properties
 */
data class PropertySpecification(

    override val title: String? = null,
    override val description: String? = null,
    override val optional: Boolean = false,
    override val default: JsonNode? = null,
    override val type: TypeSpecification? = null,
    override val format: String? = null,
    override val enum: List<JsonNode>? = null,
    override val items: PropertySpecification? = null,

    @JsonProperty("x-title-property")
    override val titleProperty: String? = null,

    @JsonProperty("x-value-property")
    override val valueProperty: String? = null,

    @JsonProperty("x-condition")
    override val condition: JsonNode? = null,

    @JsonProperty("x-short-option")
    override val shortOption: String? = null,

    @JsonProperty("x-env")
    override val env: String? = null,
) : PropertyDefinition() {

    @JsonCreator
    constructor(textValue: String) : this(type = TypeSpecification(textValue)) // Defaults to type name reference

    fun withType(type: TypeSpecification?): PropertySpecification {
        return copy(type = type)
    }
}

/**
 * Used in Prompt and ScriptInfo to define parameters
 */
data class ParameterData(

    override val title: String? = null,
    override val description: String? = null,
    override val optional: Boolean = false,
    override val default: JsonNode? = null,
    override val type: TypeSpecification? = null,
    override val format: String? = null,
    override val enum: List<JsonNode>? = null,
    override val items: ParameterData? = null,

    @JsonProperty("x-title-property")
    override val titleProperty: String? = null,

    @JsonProperty("x-value-property")
    override val valueProperty: String? = null,

    @JsonProperty("x-condition")
    override val condition: JsonNode? = null,

    @JsonProperty("x-short-option")
    override val shortOption: String? = null,

    @JsonProperty("x-env")
    override val env: String? = null,
) : PropertyDefinition() {

    @JsonCreator
    constructor(textValue: String) : this(description = textValue)  // Defaults to description
}
