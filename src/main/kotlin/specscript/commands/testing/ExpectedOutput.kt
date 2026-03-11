package specscript.commands.testing

import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.SpecScriptCommandError
import specscript.util.Json
import tools.jackson.databind.JsonNode

object ExpectedOutput : CommandHandler("Expected output", "core/testing"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {

        val output = context.output

        if (output != data) {
            val error = Json.newObject()
            error.set("expected", data)
            error.set("actual", output)
            throw SpecScriptCommandError("Unexpected output.", type = "Output", data = error)
        }

        return null
    }
}