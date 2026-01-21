package specscript.commands.files

import specscript.language.*
import specscript.util.Yaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.databind.node.ValueNode
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
        is StringNode -> {
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
