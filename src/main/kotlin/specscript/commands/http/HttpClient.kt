package specscript.commands.http

import io.ktor.http.*
import specscript.language.ScriptContext
import specscript.language.SpecScriptCommandError
import specscript.util.Json
import specscript.util.Yaml
import specscript.util.toDisplayYaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.databind.node.ValueNode
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Path

object HttpClient {

    private val client = java.net.http.HttpClient.newHttpClient()

    fun processRequest(data: ValueNode, context: ScriptContext, method: HttpMethod): JsonNode? {

        val uri = URI(encodePath(data.stringValue()))
        val separator = data.stringValue().indexOf(uri.path)
        val parsedData = Json.newObject("path", uri.toString().substring(separator))

        val url = uri.toString().substring(0, separator)
        if (url.isNotEmpty()) {
            parsedData.put("url", url)
        }

        return processRequest(parsedData, context, method)
    }

    fun processRequest(data: ObjectNode, context: ScriptContext, method: HttpMethod): JsonNode? {
        val parameters = HttpParameters.create(data, HttpRequestDefaults.getFrom(context), method)
        return processRequest(parameters)
    }

    private fun processRequest(parameters: HttpParameters): JsonNode? {

        val request = buildRequest(parameters)
        val response = client.send(request, BodyHandlers.ofByteArray())

        return handleResponse(response, parameters)
    }

    private fun buildRequest(parameters: HttpParameters): HttpRequest {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(parameters.url))
            .method(parameters.method.value, bodyPublisher(parameters))

        headers(builder, parameters)
        cookies(builder, parameters)
        basicAuth(builder, parameters)

        return builder.build()
    }

    private fun bodyPublisher(parameters: HttpParameters): HttpRequest.BodyPublisher {
        val body = parameters.body ?: return HttpRequest.BodyPublishers.noBody()

        val contentType = parameters.headers?.get("Content-Type")?.stringValue()
        if (contentType == ContentType.Application.FormUrlEncoded.toString()) {
            val formData = body.properties().joinToString("&") { entry ->
                "${URLEncoder.encode(entry.key, Charsets.UTF_8)}=${URLEncoder.encode(entry.value.toDisplayYaml(), Charsets.UTF_8)}"
            }
            return HttpRequest.BodyPublishers.ofString(formData)
        }

        return HttpRequest.BodyPublishers.ofString(body.toString())
    }

    private fun headers(builder: HttpRequest.Builder, parameters: HttpParameters) {
        parameters.headers?.properties()?.forEach { header ->
            builder.header(header.key, header.value.stringValue())
        }

        if (parameters.headers?.has("Content-Type") != true) {
            builder.header("Content-Type", ContentType.Application.Json.toString())
        }
        if (parameters.headers?.has("Accept") != true) {
            builder.header("Accept", "*/*")
        }
    }

    private fun cookies(builder: HttpRequest.Builder, parameters: HttpParameters) {
        val cookies = parameters.cookies ?: return
        val cookieHeader = cookies.properties().joinToString("; ") { "${it.key}=${it.value.stringValue()}" }
        builder.header("Cookie", cookieHeader)
    }

    private fun basicAuth(builder: HttpRequest.Builder, parameters: HttpParameters) {
        val username = parameters.username ?: return
        val password = parameters.password ?: ""
        val encoded = java.util.Base64.getEncoder()
            .encodeToString("$username:$password".toByteArray())
        builder.header("Authorization", "Basic $encoded")
    }

    private fun handleResponse(
        response: java.net.http.HttpResponse<ByteArray>,
        parameters: HttpParameters
    ): JsonNode? {

        val statusCode = response.statusCode()

        // Error
        if (statusCode !in 200..299) {
            val data = Yaml.parseIfPossible(String(response.body()))
            val type = statusCode.toString()
            throw SpecScriptCommandError("Http request returned an error", type = type, data = data)
        }

        // No content
        val body = response.body()
        if (body.isEmpty()) return null

        // Save to file
        if (parameters.saveAs != null) {
            Path.of(parameters.saveAs).toFile().writeBytes(body)
            return null
        }

        // Parse body
        return try {
            Yaml.parse(String(body))
        } catch (e: Exception) {
            StringNode(String(body))
        }
    }
}

fun encodePath(path: String?): String {
    return path?.replace(' ', '+') ?: ""
}
