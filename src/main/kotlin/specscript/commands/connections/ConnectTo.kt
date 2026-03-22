package specscript.commands.connections

import specscript.files.FileContext
import specscript.files.SpecScriptDirectories
import specscript.files.SpecScriptFile
import specscript.language.*
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ValueNode
import java.nio.file.Path
import kotlin.io.path.name

object ConnectTo : CommandHandler("Connect to", "core/connections"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        if (context !is FileContext) {
            error("'Connect to' is only supported when running files.")
        }

        // Already connected, return previous result from connection script
        val target = data.stringValue()
        if (context.getCache().contains(target)) {
            return context.getCache()[target]
        }

        // Find connection — inherited connections (first one wins) take precedence over config files
        val overrides = context.getConnectionOverrides()
        val override = overrides[target]
        if (override != null) {
            val result = connect(override, context.scriptDir, context)
            context.getCache()[target] = result
            return result
        }

        // Search config files upward from script directory
        val match = SpecScriptDirectories.findConnection(target, context.scriptDir)
            ?: throw SpecScriptException("No connection configured for $target in ${context.scriptFile.parent.toRealPath().name} or any parent directory")

        val result = connect(match.definition, match.configDir, context)
        context.getCache()[target] = result
        return result
    }


    @Suppress("UNCHECKED_CAST")
    private fun ScriptContext.getCache(): MutableMap<String, JsonNode?> {
        return this.session.getOrPut("connect-to.cache") { mutableMapOf<String, JsonNode?>() } as MutableMap<String, JsonNode?>
    }
}

/**
 * Execute a connection definition. File path references are resolved relative to [configDir].
 */
private fun connect(
    connectScript: JsonNode,
    configDir: Path,
    context: FileContext
): JsonNode? {

    when (connectScript) {
        is ValueNode -> {
            val cliFile = configDir.resolve(connectScript.stringValue())
            return SpecScriptFile(cliFile).run(FileContext(cliFile, context))
        }

        else -> {
            return connectScript.run(context)
        }
    }
}
