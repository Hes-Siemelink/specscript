package specscript.commands.datamanipulation

import specscript.commands.datamanipulation.Add.add
import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.util.toArrayNode
import tools.jackson.databind.JsonNode

object Append : CommandHandler("Append", "core/data-manipulation"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode {

        var total = context.output ?: return data

        for (item in data.toArrayNode()) {
            total = add(total, item)
        }

        return total
    }
}

