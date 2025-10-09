package specscript.commands.files

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.ValueNode
import com.fasterxml.jackson.module.kotlin.contains
import specscript.language.*
import specscript.util.Yaml
import java.nio.file.Path
import kotlin.io.path.exists

object ReadFile : CommandHandler("Read file", "core/files"), ValueHandler, ObjectHandler {

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode {
        val file = data.toPath(context)

        return Yaml.readFile(file)
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode {
        val file = data.toPath(context)

        return Yaml.readFile(file)
    }
}

fun JsonNode.toPath(context: ScriptContext, directory: Path? = null): Path {
    return when (this) {
        is TextNode -> {
            val dir = directory ?: context.workingDir
            val file = dir.resolve(textValue())
            if (file.exists()) {
                file
            } else {
                throw CommandFormatException("File not found: ${file.toRealPath()}")
            }
        }

        is ObjectNode -> {
            if (contains("file")) {
                this["file"].toPath(context)
            } else if (contains("resource")) {
                this["resource"].toPath(context, context.scriptDir)
            } else {
                throw CommandFormatException("Expected either 'file' or 'resource' property.")
            }
        }

        else -> throw CommandFormatException("Unsupported node type for files: ${javaClass.simpleName}")
    }
}
