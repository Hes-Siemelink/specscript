package specscript.commands.files

import specscript.files.SpecScriptFile
import specscript.language.*
import specscript.util.Json
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

object RunScript : CommandHandler("Run script", "core.files"), ObjectHandler, ValueHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val file = data.toPath(context)
        val input = data["input"] ?: Json.newObject()

        return runCommand(SpecScriptFile(file), input, context)
    }

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        val file = data.toPath(context, context.scriptDir)

        return handleCommand(SpecScriptFile(file), Json.newObject(), context)
    }
}