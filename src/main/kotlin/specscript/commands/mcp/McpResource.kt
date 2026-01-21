package specscript.commands.mcp

import com.fasterxml.jackson.annotation.JsonAnySetter
import specscript.language.CommandHandler
import specscript.language.DelayedResolver
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

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