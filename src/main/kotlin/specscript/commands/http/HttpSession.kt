package specscript.commands.http

import specscript.language.*
import specscript.util.Json
import specscript.util.withoutSecrets
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

object HttpSession : CommandHandler("Http session", "http/client"), ObjectHandler, ValueHandler {

    internal val sessions = SessionRegistry<HttpSessionData>("http.sessions", "http-session")

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {
        val name = data["name"]?.stringValue() ?: sessions.generateName(context)
        data.put("name", name)

        sessions.open(context, HttpSessionData(name, data))

        return data.withoutSecrets()
    }

    override fun execute(
        data: ValueNode,
        context: ScriptContext
    ): JsonNode {

        // Passing empty string means get current session.
        val targetName = data.stringValue()
        if (targetName.isBlank()) {
            return sessions.current(context)?.data?.withoutSecrets() ?: Json.newObject()
        }

        // Switch to target or return empty object if it doesn't exist.
        val target = sessions.get(context, targetName)
        if (target == null) {
            return Json.newObject()
        } else {
            sessions.setCurrentSession(context, targetName)
            return target.data.withoutSecrets()
        }
    }
}

object HttpCloseSession : CommandHandler("Http close session", "http/client"), ValueHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        HttpSession.sessions.close(context, data.stringValue())
        return null
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        HttpSession.sessions.close(context, data.getTextParameter("name"))
        return null
    }
}

class HttpSessionData(override val name: String, override val data: ObjectNode) : Session
