package specscript.commands.datamanipulation

import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.commands.datamanipulation.Add.add
import specscript.language.*
import specscript.util.asArray

object AddTo : CommandHandler("Add to", "core/data-manipulation"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): Nothing? {

        for ((key, value) in data.fields()) {
            val match = VARIABLE_REGEX.matchEntire(key)
                ?: throw CommandFormatException("Entries should be in \${..} variable syntax.")
            val varName = match.groupValues[1]

            var total = context.variables[varName] ?: throw ScriptingException("Variable $varName not found.")
            for (item in value.asArray()) {
                total = add(total, item)
            }
            context.variables[varName] = total
        }

        return null
    }
}