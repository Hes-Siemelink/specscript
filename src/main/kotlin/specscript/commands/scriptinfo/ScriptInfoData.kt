package specscript.commands.scriptinfo

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import specscript.language.types.ObjectDefinition
import specscript.language.types.ParameterData
import specscript.language.types.PropertyDefinition
import specscript.language.types.TypeSpecification

data class ScriptInfoData(
    val description: String? = null,
    val input: Map<String, ParameterData>? = null,
    @JsonProperty("input type")
    val inputType: TypeSpecification? = null,
    val hidden: Boolean = false,
    @JsonProperty("specscript-version")
    val specScriptVersion: String? = null
) : ObjectDefinition {

    @JsonCreator
    constructor(textValue: String) : this(description = textValue)

    override val properties: Map<String, PropertyDefinition>
        get() = input ?: inputType?.properties?.properties ?: emptyMap()
}