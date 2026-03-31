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
import specscript.commands.server.HandlerInfo
import specscript.commands.server.run
import specscript.language.*
import specscript.util.Json
import specscript.util.Yaml
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import kotlin.concurrent.thread


object HttpServer : CommandHandler("Http server", "core/http"), ObjectHandler, DelayedResolver {

    init {
        // Avoid NoClassDefFoundError from Ktor's shutdown hook when the JVM exits via Ctrl+C
        System.setProperty("io.ktor.server.engine.ShutdownHook", "false")
    }

    private const val DEFAULT_HTTP_SERVER = "http.server.default"

    // Active servers keyed by name
    private val servers = mutableMapOf<String, HttpServerInstance>()
    private val keepAliveThreads = mutableMapOf<String, Thread>()

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(HttpServerInfo::class)

        setDefaultServer(context, info.name)

        // Eagerly start the server so Http endpoint can add to it later
        ensureRunning(info.name, info.port)

        // Register endpoints
        info.endpoints.paths.forEach { (path, endpointData) ->
            installEndpoint(servers[info.name]!!, path, endpointData, context)
        }
        return null
    }

    fun stop(name: String) {
        println("Stopping SpecScript Http Server '$name'")
        keepAliveThreads.remove(name)?.interrupt()
        servers.remove(name)?.stop(100, 200)
    }

    fun getDefaultServerName(context: ScriptContext): String {
        return context.session[DEFAULT_HTTP_SERVER] as? String
            ?: throw SpecScriptException("No HTTP server found in current context. An Http server must be started before defining endpoints.")
    }

    private fun setDefaultServer(context: ScriptContext, serverName: String) {
        context.session[DEFAULT_HTTP_SERVER] = serverName
    }

    private fun ensureRunning(serverName: String, port: Int): HttpServerInstance {
        return servers.getOrPut(serverName) {
            println("Starting SpecScript Http Server '$serverName' on port $port")
            embeddedServer(Netty, port = port) { }.also {
                it.start(wait = false)
                keepAliveThreads[serverName] = thread(isDaemon = false, name = "HTTP keep-alive - $serverName") {
                    try { Thread.currentThread().join() } catch (_: InterruptedException) { }
                }
            }
        }
    }

    fun addHandler(serverName: String, port: Int, rawPath: String, data: EndpointData, context: ScriptContext) {
        val server = ensureRunning(serverName, port)
        installEndpoint(server, rawPath, data, context)
    }

    fun addEndpoint(serverName: String, rawPath: String, data: EndpointData, context: ScriptContext) {
        val server = servers[serverName]
            ?: throw SpecScriptException("HTTP server '$serverName' is not running. Start it with Http server before adding endpoints.")
        installEndpoint(server, rawPath, data, context)
    }

    private fun installEndpoint(server: HttpServerInstance, rawPath: String, data: EndpointData, context: ScriptContext) {
        // Normalize ":id" path parameters into Ktor style "{id}"
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

    // Resolve output via (priority) output -> script
    val output = data.run(localContext)

    // Return JSON (output already a JsonNode)
    output?.let { call.respondText(it.toString(), ContentType.Application.Json) }
}

private fun ScriptContext.addInputVariable(call: ApplicationCall, bodyText: String) {
    // Body takes precedence
    if (bodyText.isNotBlank()) {
        variables[INPUT_VARIABLE] = Yaml.parseIfPossible(bodyText)
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
    if (isBlank()) Json.newObject() else runCatching { Json.readJson(this) }.getOrElse { StringNode(this) }


private typealias HttpServerInstance = EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

data class HttpServerInfo(
    val name: String,
    val port: Int = 3000,
    val endpoints: Endpoints = Endpoints()
)

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
    override val output: JsonNode? = null,
    override val script: JsonNode? = null
) : HandlerInfo {
    @JsonCreator
    constructor(textValue: String) : this(script = StringNode(textValue))
}
