package specscript.commands.mcp

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.*
import specscript.util.*

object McpTool : CommandHandler("Mcp tool", "ai/mcp"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        // Parse tools data - similar to how McpServer handles tools
        val toolsData = data.toDomainObject(McpToolsData::class)
        
        // Get the current server using encapsulated McpServer logic
        val server = McpServer.getCurrentServer(context)
            ?: throw IllegalStateException("No MCP server context found. An MCP server must be started before defining tools.")
        
        // Add each tool to the running server using shared extension method
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

/**
 * Data class for Mcp tool command - handles multiple tools with names as keys.
 * Good OO / encapsulation: Reuses existing ToolInfo and follows same pattern as McpServer.
 */
class McpToolsData {
    @JsonAnySetter
    val tools: MutableMap<String, ToolInfo> = mutableMapOf()
}
