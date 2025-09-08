package specscript.commands.testing

import com.fasterxml.jackson.databind.JsonNode
import specscript.language.AnyHandler
import specscript.language.CommandHandler
import specscript.language.SpecScriptCommandError
import specscript.language.ScriptContext
import specscript.util.Json

object ExpectedOutput : CommandHandler("Expected output", "core/testing"), AnyHandler {

    override fun execute(data: JsonNode, context: ScriptContext): JsonNode? {

        val output = context.output

        if (output != data) {
            val error = Json.newObject()
            error.set<JsonNode>("expected", data)
            error.set<JsonNode>("actual", output)
            throw SpecScriptCommandError("Output", "Unexpected output.", error)
        }

        return null
    }
}