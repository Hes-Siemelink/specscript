package specscript.commands.files

import specscript.files.FileContext
import specscript.files.SpecScriptFile
import specscript.language.*
import specscript.util.Json
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.databind.node.ValueNode
import java.nio.file.Path
import kotlin.io.path.exists

object Run : CommandHandler("Run", "core/files"), ObjectHandler, ValueHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {

        val cd = data["cd"]?.resolve(context)?.optionalText()?.let { Path.of(it) }
        val scriptNode = data["script"]
        val fileNode = data["file"]

        return when {
            fileNode != null -> {
                val baseDir = cd ?: context.workingDir
                val file = baseDir.resolve(fileNode.resolve(context).text()).also { it.requireExists() }
                val input = data["input"]?.resolve(context) ?: Json.newObject()
                runScriptFile(file, input, context)
            }

            scriptNode is StringNode -> {
                val resolvedScript = scriptNode.resolve(context)
                val baseDir = cd ?: context.scriptDir
                val file = baseDir.resolve(resolvedScript.text()).also { it.requireExists() }
                val input = data["input"]?.resolve(context) ?: Json.newObject()
                runScriptFile(file, input, context)
            }

            scriptNode != null -> {
                // Inline script — do NOT resolve the script body; it runs in its own context
                runInlineScript(scriptNode, cd, context)
            }

            else -> throw CommandFormatException("Expected 'script' or 'file' property")
        }
    }

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        val resolved = data.resolve(context)
        val file = context.scriptDir.resolve(resolved.text()).also { it.requireExists() }

        return runScriptFile(file, Json.newObject(), context)
    }

    private fun runScriptFile(file: Path, input: JsonNode, context: ScriptContext): JsonNode? {
        return runCommand(SpecScriptFile(file), input, context)
    }

    private fun runInlineScript(scriptNode: JsonNode, cd: Path?, context: ScriptContext): JsonNode? {

        val scriptDir = cd ?: context.scriptDir
        val childContext = FileContext(
            scriptFile = scriptDir,
            variables = mutableMapOf(),
            session = context.session,
            interactive = context.interactive,
            workingDir = cd ?: context.workingDir
        )

        // Delegate command lookup to the parent context so inline scripts
        // can use the host script's local commands, imports, and connections
        if (context is FileContext) {
            childContext.parentCommandLookup = context
        }

        return scriptNode.run(childContext)
    }
}

private fun Path.requireExists() {
    if (!exists()) {
        throw CommandFormatException("File not found: ${toAbsolutePath()}")
    }
}

private fun JsonNode.text(): String {
    return stringValue() ?: throw CommandFormatException("Expected text value")
}

private fun JsonNode.optionalText(): String? {
    return stringValue()
}
