package specscript.commands.mcp

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.CommandHandler
import specscript.language.DelayedResolver
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.util.toDomainObject

object McpResource : CommandHandler("Mcp resource", "ai/mcp"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val resourcesData = data.toDomainObject(McpResourcesData::class)
        val server = McpServer.getDefaultServer(context)

        // Add each resource to the running server
        resourcesData.resources.forEach { (resourceURI, resourceInfo) ->
            with(McpServer) {
                server.addResource(resourceURI, resourceInfo, context.clone())
            }
        }

        return null
    }
}

class McpResourcesData {
    @JsonAnySetter
    val resources: MutableMap<String, ResourceInfo> = mutableMapOf()
}