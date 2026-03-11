package specscript.commands.http

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonCreator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import specscript.files.SpecScriptFile
import specscript.language.*
import specscript.util.Json
import specscript.util.Yaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import kotlin.io.path.name


object HttpServer : CommandHandler("Http server", "core/http"), ObjectHandler, DelayedResolver {

    // Active servers
    private val servers = mutableMapOf<Int, HttpServerInstance>()

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val port = data.getParameter("port").intValue()

        // Stop request
        data["stop"]?.let {
            if (it.booleanValue()) {
                stop(port); return null
            }
        }

        // Register endpoints
        val serveData: Endpoints = Yaml.parse(data.getParameter("endpoints"))
        serveData.paths.forEach { (path, endpointData) -> addHandler(port, path, endpointData, context) }
        return null
    }

    fun stop(port: Int) {
        println("Stopping SpecScript Http Server on port $port")
        // Immediate shutdown (no graceful delay) for fast test cycles
        servers.remove(port)?.stop(0, 0)
    }

    private fun addHandler(port: Int, rawPath: String, data: EndpointData, context: ScriptContext) {
        // Start (or reuse) server for this port
        val server = servers.getOrPut(port) {
            println("Starting SpecScript Http Server for ${context.scriptFile.name} on port $port")
            embeddedServer(Netty, port = port) { }.also { it.start(wait = false) }
        }

        // Normalize Javalin style ":id" into Ktor style "{id}" so existing specs continue to work.
        val normalizedPath = normalizePath(rawPath)
        val pathParamNames = extractPathParamNames(normalizedPath)

        // Install all method handlers for this path in one routing block.
        server.application.routing {
            route(normalizedPath) {
                data.methodHandlers.forEach { (methodName, handlerData) ->
                    val httpMethod = methods[methodName]
                        ?: throw CommandFormatException("Unsupported HTTP method: $methodName")
                    method(httpMethod) {
                        handle { handleRequest(handlerData, call, context, pathParamNames) }
                    }
                }
            }
        }
    }
}

private fun normalizePath(path: String): String =
    path.replace(Regex(":(\\w+)")) { "{" + it.groupValues[1] + "}" }

private fun extractPathParamNames(path: String): List<String> =
    Regex("\\{(\\w+)}").findAll(path).map { it.groupValues[1] }.toList()

private val methods = mapOf(
    "get" to HttpMethod.Get,
    "post" to HttpMethod.Post,
    "put" to HttpMethod.Put,
    "patch" to HttpMethod.Patch,
    "delete" to HttpMethod.Delete
)

private suspend fun handleRequest(
    data: MethodHandlerData,
    call: ApplicationCall,
    scriptContext: ScriptContext,
    pathParamNames: List<String>
) {
    val localContext = scriptContext.clone()

    // Read body once (important: receiveText() consumes the channel)
    val bodyText = runCatching { call.receiveText() }.getOrNull().orEmpty()

    // Populate script context variables used by downstream scripts
    localContext.addInputVariable(call, bodyText)
    localContext.addRequestVariable(call, bodyText, pathParamNames)

    // Resolve output via (priority) output -> script -> file
    val output = when {
        data.output != null -> data.output.resolve(localContext)
        data.script != null -> data.script.run(localContext)
        data.file != null -> SpecScriptFile(localContext.scriptDir.resolve(data.file)).run(localContext)
        else -> throw SpecScriptException("No handler action defined")
    }

    // Return JSON (output already a JsonNode)
    output?.let { call.respondText(it.toString(), ContentType.Application.Json) }
}

private fun ScriptContext.addInputVariable(call: ApplicationCall, bodyText: String) {
    // Body takes precedence
    if (bodyText.isNotBlank()) {
        variables[INPUT_VARIABLE] = runCatching { Json.mapper.readTree(bodyText) }.getOrElse { StringNode(bodyText) }
        return
    }
    // Fallback to query parameters if present
    val qp = call.request.queryParameters
    if (!qp.isEmpty()) {
        variables[INPUT_VARIABLE] = Json.newObject(qp.names().associateWith { qp[it] ?: "" })
    }
}

private fun ScriptContext.addRequestVariable(
    call: ApplicationCall,
    bodyText: String,
    pathParamNames: List<String>
) {
    val requestData = Json.newObject()
    requestData.set("headers", call.headersAsJson())
    requestData.set("path", StringNode(call.request.path()))
    requestData.set("pathParameters", call.pathParametersAsJson(pathParamNames))
    requestData.set("query", StringNode(call.request.queryString().orEmpty()))
    requestData.set("queryParameters", call.queryParametersAsJson())
    requestData.set("body", bodyText.toBodyJson())
    requestData.set("cookies", call.cookiesAsJson())
    variables["request"] = requestData
}

private fun ApplicationCall.headersAsJson(): ObjectNode =
    Json.newObject(request.headers.names().associateWith { request.headers[it] ?: "" })

private fun ApplicationCall.pathParametersAsJson(pathParamNames: List<String>): ObjectNode =
    Json.newObject(pathParamNames.associateWith { parameters[it] ?: "" })

private fun ApplicationCall.queryParametersAsJson(): ObjectNode =
    Json.newObject(request.queryParameters.names().associateWith { request.queryParameters[it] ?: "" })

private fun ApplicationCall.cookiesAsJson(): ObjectNode =
    Json.newObject(request.cookies.rawCookies)

private fun String.toBodyJson(): JsonNode =
    if (isBlank()) Json.newObject() else runCatching { Json.mapper.readTree(this) }.getOrElse { StringNode(this) }


private typealias HttpServerInstance = EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

class Endpoints {
    @JsonAnyGetter
    @JsonAnySetter
    val paths: MutableMap<String, EndpointData> = linkedMapOf()
}

class EndpointData {
    @JsonAnyGetter
    @JsonAnySetter
    val methodHandlers: MutableMap<String, MethodHandlerData> = mutableMapOf()
}

data class MethodHandlerData(
    val output: JsonNode? = null,
    val script: JsonNode? = null,
    val file: String? = null
) {
    @JsonCreator
    constructor(textValue: String) : this(file = textValue)
}
