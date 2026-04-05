package specscript.commands.files

import specscript.language.*
import specscript.util.toDisplayYaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createParentDirectories

object WriteFile : CommandHandler("Write file", "core/files"), ValueHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        val filename = data.stringValue()
        val content = context.output ?: throw SpecScriptCommandError(
            "Write file requires 'content' parameter or non-null output variable."
        )
        writeFile(filename, content, context)

        return null
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val filename = data.getParameter("file").stringValue()
        val content = data["content"] ?: context.output ?: throw SpecScriptCommandError(
            "Write file requires 'content' parameter or non-null output variable."
        )

        writeFile(filename, content, context)

        return null
    }

    private fun writeFile(filename: String, content: JsonNode, context: ScriptContext) {
        val destinationFile = context.workingDir.resolve(filename)
        destinationFile.createParentDirectories()
        Files.writeString(destinationFile, content.toDisplayYaml())
    }
}