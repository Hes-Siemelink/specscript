package specscript.commands.connections

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.files.FileContext
import specscript.files.SpecScriptFile
import specscript.language.*
import kotlin.io.path.name

object ConnectTo : CommandHandler("Connect to", "core/connections"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        if (context !is FileContext) {
            error("'Connect to' is only supported when running files.")
        }

        val targetName = data.textValue()
        val connectScript = context.info.connections[targetName]
            ?: throw ScriptingException("No connection script configured for $targetName in ${context.scriptFile.parent.toRealPath().name}")

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
}