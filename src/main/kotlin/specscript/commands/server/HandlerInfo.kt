package specscript.commands.server

import specscript.files.FileContext
import specscript.files.SpecScriptFile
import specscript.language.ScriptContext
import specscript.language.SpecScriptException
import specscript.language.resolve
import specscript.language.run
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode

/**
 * Common interface for handler definitions that resolve to output via `output` or `script`.
 */
interface HandlerInfo {
    val output: JsonNode?
    val script: JsonNode?
}

/**
 * Runs the handler. Priority: output (resolved) > script file (string) > inline script (object).
 */
fun HandlerInfo.run(context: ScriptContext): JsonNode? {
    return when {
        output != null -> output!!.resolve(context)
        script is StringNode -> {
            val file = context.scriptDir.resolve((script as StringNode).stringValue())
            SpecScriptFile(file).run(FileContext(file, context, context.variables))
        }

        script != null -> script!!.run(context)
        else -> throw SpecScriptException("No handler action defined — provide output or script")
    }
}
