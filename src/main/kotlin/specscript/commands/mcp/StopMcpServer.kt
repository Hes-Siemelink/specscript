package specscript.commands.mcp

import specscript.language.CommandHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ValueNode

object StopMcpServer : CommandHandler("Stop mcp server", "ai/mcp"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        McpServer.stopServer(data.stringValue())
        McpServer.clearCurrentServer(context)
        return null
    }
}
