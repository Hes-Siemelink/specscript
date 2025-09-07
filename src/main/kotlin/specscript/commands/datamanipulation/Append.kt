package specscript.commands.datamanipulation

import com.fasterxml.jackson.databind.JsonNode
import specscript.commands.datamanipulation.Add.add
import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.util.asArray

object Append : CommandHandler("Append", "core/data-manipulation"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode {

        var total = context.output ?: return data

        for (item in data.asArray()) {
            total = add(total, item)
        }

        return total
    }
}

