package specscript.commands.http

import io.ktor.http.*
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

object Patch : CommandHandler("PATCH", "core/http"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        return HttpClient.processRequest(data, context, HttpMethod.Patch)
    }
}