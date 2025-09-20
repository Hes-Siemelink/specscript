package specscript.commands.files

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.files.SpecScriptFile
import specscript.language.*
import specscript.util.Json

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