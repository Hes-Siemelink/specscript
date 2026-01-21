package specscript.commands.http

import io.ktor.http.*
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

object Post : CommandHandler("POST", "core/http"), ObjectHandler, ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        return HttpClient.processRequest(data, context, HttpMethod.Post)
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        return HttpClient.processRequest(data, context, HttpMethod.Post)
    }
}