package specscript.commands.mcp

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.*
import specscript.util.*

object McpTool : CommandHandler("Mcp tool", "ai/mcp"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val toolsData = data.toDomainObject(McpToolsData::class)
        val server = McpServer.getCurrentServer(context)

        // Add each tool to the running server
        toolsData.tools.forEach { (toolName, toolInfo) ->
            // TODO: Check if tools can be added to running servers dynamically. 
            // The MCP Kotlin SDK may require server restart to add new tools.
            // For now, this assumes addTool works on running servers.
            with(McpServer) {
                server.addTool(toolName, toolInfo, context.clone())
            }
        }
        
        return null
    }
}

class McpToolsData {
    @JsonAnySetter
    val tools: MutableMap<String, ToolInfo> = mutableMapOf()
}
