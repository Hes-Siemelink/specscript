package specscript.commands.files

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import specscript.language.*
import specscript.util.toDisplayYaml
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createParentDirectories

object WriteFile : CommandHandler("Write file", "core/files"), ValueHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {

        val filename = data.textValue()
        val content = context.output ?: throw SpecScriptCommandError(
            "Write file requires 'content' parameter or non-null output variable."
        )
        writeFile(filename, content)

        return null
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val filename = data.getParameter("file").textValue()
        val content = data["content"] ?: context.output ?: throw SpecScriptCommandError(
            "Write file requires 'content' parameter or non-null output variable."
        )

        writeFile(filename, content)

        return null
    }

    private fun writeFile(filename: String, content: JsonNode) {
        val destinationFile = Path.of(filename)
        destinationFile.createParentDirectories()
        Files.writeString(destinationFile, content.toDisplayYaml())
    }
}