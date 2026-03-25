package specscript.commands.http

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import specscript.language.*
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

object HttpEndpoint : CommandHandler("Http endpoint", "core/http"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val endpointsData = data.toDomainObject(HttpEndpointData::class)
        val serverName = HttpServer.getDefaultServerName(context)

        endpointsData.paths.forEach { (path, endpointData) ->
            HttpServer.addEndpoint(serverName, path, endpointData, context)
        }

        return null
    }
}

class HttpEndpointData {
    @JsonAnyGetter
    @JsonAnySetter
    val paths: MutableMap<String, EndpointData> = linkedMapOf()
}
