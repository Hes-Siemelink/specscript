package specscript.commands.connections

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.files.FileContext
import specscript.files.SpecScriptFile
import specscript.language.*
import kotlin.io.path.name

object ConnectTo : CommandHandler("Connect to", "core/connections"), ValueHandler {

    val alreadyConnected: MutableMap<String, JsonNode?> = mutableMapOf()

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        if (context !is FileContext) {
            error("'Connect to' is only supported when running files.")
        }

        // Already connected, return previous result from connection script
        val target = data.textValue()
        if (alreadyConnected.contains(target)) {
            return alreadyConnected[target]
        }

        // Find script
        val connectScript = context.info.connections[target]
            ?: throw ScriptingException("No connection script configured for $target in ${context.scriptFile.parent.toRealPath().name}")

        // Execute script
        val result = connect(connectScript, context)

        alreadyConnected[target] = result

        return result
    }
}

private fun connect(
    connectScript: JsonNode,
    context: FileContext
): JsonNode? {

    when (connectScript) {
        is ValueNode -> {
            val cliFile = context.scriptDir.resolve(connectScript.textValue())
            return SpecScriptFile(cliFile).run(FileContext(cliFile, context))
        }

        else -> {
            return connectScript.run(context)
        }
    }
}