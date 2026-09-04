package specscript.commands.http

import specscript.language.*
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

object HttpSession : CommandHandler("Http session", "core/http"), ObjectHandler {

    internal val sessions = SessionRegistry<HttpSessionData>("http.sessions", "http-session")

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {
        val name = data["name"]?.stringValue() ?: sessions.generateName(context)
        data.put("name", name)

        sessions.open(context, HttpSessionData(name, data))

        return data
    }
}

object HttpCloseSession : CommandHandler("Http close session", "core/http"), ValueHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        HttpSession.sessions.close(context, data.stringValue())
        return null
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        HttpSession.sessions.close(context, data.getTextParameter("name"))
        return null
    }
}

class HttpSessionData(override val name: String, val data: ObjectNode) : Session
