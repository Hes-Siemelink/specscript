package specscript.cli

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import specscript.language.types.ObjectDefinition
import specscript.language.types.ParameterData

class CommandLineParameters : ObjectDefinition {

    @JsonAnyGetter
    @JsonAnySetter
    override val properties: MutableMap<String, ParameterData> = LinkedHashMap()

    fun contains(option: String): Boolean {
        return properties.contains(option)
    }

    companion object {
        operator fun invoke(properties: Map<String, ParameterData>): CommandLineParameters {
            return CommandLineParameters().also { it.properties.putAll(properties) }
        }
    }
}
