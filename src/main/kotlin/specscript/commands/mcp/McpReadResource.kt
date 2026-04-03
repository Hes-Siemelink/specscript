package specscript.commands.mcp

import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.coroutines.runBlocking
import specscript.language.*
import specscript.util.Yaml
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

object McpReadResource : CommandHandler("Mcp read resource", "ai/mcp"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(ReadMcpResourceInfo::class)

        return runBlocking {
            readResource(info)
        }
    }

    private suspend fun readResource(info: ReadMcpResourceInfo): JsonNode? {
        val mcp = createMcpClient(info.server)

        return try {
            mcp.connect()

            val request = ReadResourceRequest(
                ReadResourceRequestParams(uri = info.uri)
            )

            val result = mcp.client.readResource(request)
            val first = result.contents.firstOrNull()
                ?: return StringNode("Resource returned no content")

            when (first) {
                is TextResourceContents -> Yaml.parseIfPossible(first.text)
                else -> StringNode("Resource returned content of unsupported type")
            }

        } catch (e: Exception) {
            throw SpecScriptCommandError("Resource '${info.uri}' read failed: ${e.message}", cause = e)
        } finally {
            mcp.close()
        }
    }
}

data class ReadMcpResourceInfo(
    val uri: String,
    val server: TargetServerInfo,
)
