package specscript.commands.mcp

import kotlinx.coroutines.runBlocking
import specscript.commands.mcp.transport.McpClientWrapper
import specscript.language.*
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

object McpSession : CommandHandler("Mcp session", "mcp/client"), ObjectHandler {

    internal val sessions = SessionRegistry<McpSessionData>("mcp.sessions", "mcp-session")

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {
        val name = data["name"]?.stringValue() ?: sessions.generateName(context)
        data.put("name", name)

        val serverData = data.deepCopy()
        serverData.remove("name")
        val server = serverData.toDomainObject(TargetServerInfo::class)

        val client = createMcpClient(server)
        try {
            runBlocking { client.connect() }
        } catch (e: Exception) {
            throw SpecScriptCommandError("Could not open Mcp session '$name': ${e.message}", cause = e)
        }

        sessions.open(context, McpSessionData(name, data, client))

        return data
    }
}

object McpCloseSession : CommandHandler("Mcp close session", "mcp/client"), ValueHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        McpSession.sessions.close(context, data.stringValue())
        return null
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        McpSession.sessions.close(context, data.getTextParameter("name"))
        return null
    }
}

class McpSessionData(
    override val name: String,
    override val data: ObjectNode,
    val client: McpClientWrapper
) : Session {

    override fun close() {
        runBlocking { client.close() }
    }
}
