package specscript.commands.connections

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.files.CliFile
import specscript.files.CliFileContext
import specscript.language.*
import kotlin.io.path.name

object ConnectTo : CommandHandler("Connect to", "core/connections"), ValueHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        if (context !is CliFileContext) {
            error("'Connect to' is only supported when running files.")
        }

        val targetName = data.textValue()
        val connectScript = context.info.connections[targetName]
            ?: throw CliScriptingException("No connection script configured for $targetName in ${context.cliFile.parent.toRealPath().name}")

        when (connectScript) {
            is ValueNode -> {
                val cliFile = context.scriptDir.resolve(connectScript.textValue())
                return CliFile(cliFile).run(CliFileContext(cliFile, context))
            }

            else -> {
                return connectScript.run(context)
            }
        }
    }
}