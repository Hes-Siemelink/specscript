package specscript.commands.mcp

import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.coroutines.runBlocking
import specscript.commands.mcp.transport.McpClientWrapper
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.SpecScriptCommandError
import specscript.util.Yaml
import specscript.util.toDomainObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

object McpReadResource : CommandHandler("Mcp read resource", "ai/mcp"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val info = data.toDomainObject(ReadMcpResourceInfo::class)

        val target = resolveMcpTarget(info.session, info.server != null, "Mcp read resource", context)

        return runBlocking {
            when (target) {
                is McpTarget.Session -> readResource(target.session.client, info)

                McpTarget.ConnectPerCall -> readResourceOnce(info)
            }
        }
    }

    private suspend fun readResourceOnce(info: ReadMcpResourceInfo): JsonNode? {
        val mcp = createMcpClient(info.server!!)

        return try {
            try {
                mcp.connect()
            } catch (e: Exception) {
                throw SpecScriptCommandError("Resource '${info.uri}' read failed: ${e.message}", cause = e)
            }

            readResource(mcp, info)
        } finally {
            mcp.close()
        }
    }

    private suspend fun readResource(mcp: McpClientWrapper, info: ReadMcpResourceInfo): JsonNode? {
        return try {
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
        }
    }
}

data class ReadMcpResourceInfo(
    val uri: String,
    val server: TargetServerInfo? = null,
    val session: String? = null,
)
