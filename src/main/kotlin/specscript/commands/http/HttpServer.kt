package specscript.commands.http

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
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
        else -> throw ScriptingException("No handler action defined")
    }

    // Return JSON (output already a JsonNode)
    output?.let { call.respondText(it.toString(), ContentType.Application.Json) }
}

private fun ScriptContext.addInputVariable(call: ApplicationCall, bodyText: String) {
    // Body takes precedence
    if (bodyText.isNotBlank()) {
        variables[INPUT_VARIABLE] = runCatching { Json.mapper.readTree(bodyText) }.getOrElse { TextNode(bodyText) }
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
    requestData.set<JsonNode>("headers", call.headersAsJson())
    requestData.set<JsonNode>("path", TextNode(call.request.path()))
    requestData.set<JsonNode>("pathParameters", call.pathParametersAsJson(pathParamNames))
    requestData.set<JsonNode>("query", TextNode(call.request.queryString().orEmpty()))
    requestData.set<JsonNode>("queryParameters", call.queryParametersAsJson())
    requestData.set<JsonNode>("body", bodyText.toBodyJson())
    requestData.set<JsonNode>("cookies", call.cookiesAsJson())
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
    if (isBlank()) Json.newObject() else runCatching { Json.mapper.readTree(this) }.getOrElse { TextNode(this) }


private typealias HttpServerInstance = EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

data class Endpoints(
    @get:JsonAnyGetter
    val paths: MutableMap<String, EndpointData> = mutableMapOf()
)

data class EndpointData(
    @get:JsonAnyGetter
    val methodHandlers: Map<String, MethodHandlerData> = mutableMapOf()
)

data class MethodHandlerData(
    val output: JsonNode? = null,
    val script: JsonNode? = null,
    val file: String? = null
) {
    @JsonCreator
    constructor(textValue: String) : this(file = textValue)
}
