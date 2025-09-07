package specscript.commands.http

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import io.ktor.http.*

object Delete : CommandHandler("DELETE", "core/http"), ValueHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        return HttpClient.processRequest(data, context, HttpMethod.Delete)
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        return HttpClient.processRequest(data, context, HttpMethod.Delete)
    }
}