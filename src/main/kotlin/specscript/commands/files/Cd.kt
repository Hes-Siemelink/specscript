package specscript.commands.files

import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ValueNode
import java.nio.file.Path

object Cd : CommandHandler("Cd", "core/files"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        context.workingDir = Path.of(data.stringValue())
        return null
    }
}
