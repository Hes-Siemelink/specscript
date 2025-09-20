package specscript.files

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import specscript.language.*
import specscript.util.Yaml
import java.nio.file.Path
import kotlin.io.path.name

class SpecScriptFile(val file: Path) : CommandInfo, CommandHandler(asScriptCommand(file.name), null), ObjectHandler {

    override val name: String = asCliCommand(file.name)
    override val description: String by lazy {
        markdown?.description
            ?: script.info.description
            ?: asScriptCommand(name)
    }
    override val hidden: Boolean by lazy { script.info.hidden }
    override val specScriptVersion: String by lazy { script.info.specScriptVersion ?: "unknown" }

    val script by lazy {
        markdown?.blocks?.toScript()
            ?: Script.from(scriptNodes)
    }
    private val scriptNodes: List<JsonNode> by lazy { Yaml.parse(file) }

    val markdown: SpecScriptMarkdown? by lazy {
        if (file.isMarkdownScript()) {
            SpecScriptMarkdown.scan(file)
        } else {
            null
        }
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val input = mutableMapOf<String, JsonNode>(INPUT_VARIABLE to data)
        val localContext = FileContext(file, context, variables = input)

        return script.run(localContext)
    }

    fun run(context: ScriptContext = FileContext(file)): JsonNode? {
        return script.run(context)
    }
}

private fun Path.isMarkdownScript(): Boolean {
    return this.name.endsWith(".md")
}

fun SpecScriptFile.splitMarkdown(): List<Script> {

    val document = markdown ?: return listOf()

    val all = mutableListOf<List<MarkdownBlock>>()
    var currentCase = mutableListOf<MarkdownBlock>()

    for (block in document.blocks) {
        when (block.type) {
            MarkdownBlock.Header -> {
                // Add previous case
                if (currentCase.isNotEmpty()) {
                    all.add(currentCase)
                }
                currentCase = mutableListOf(block)
            }

            else -> {
                currentCase.add(block)
            }
        }
    }

    // Add the last one
    all.add(currentCase)

    return all.map { it.toScript() }
}
