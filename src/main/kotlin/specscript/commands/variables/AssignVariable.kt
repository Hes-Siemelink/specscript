package specscript.commands.variables

import com.fasterxml.jackson.databind.JsonNode
import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext

class AssignVariable(private val varName: String) : CommandHandler("\${}", null), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {

        context.variables[varName] = data

        return null
    }
}