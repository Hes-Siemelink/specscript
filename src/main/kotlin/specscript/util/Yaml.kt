package specscript.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import java.nio.file.Path

object Yaml {

    private val factory = YAMLFactory()
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)

    // Bare mapper for tree-model operations (parsing YAML to/from JsonNode).
    // No KotlinModule — avoids expensive kotlin-reflect initialization on startup.
    private val treeMapper = ObjectMapper(factory).apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    // Full mapper with KotlinModule, only for typed deserialization (treeToValue, readValue<T>, valueToTree).
    // Lazy to defer kotlin-reflect cost until first actual use.
    val mapper: ObjectMapper by lazy {
        ObjectMapper(factory).registerModule(KotlinModules.module).apply {
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    fun readFile(source: Path): JsonNode {
        return treeMapper.readValue(source.toFile(), JsonNode::class.java)
    }

    fun readResource(classpathResource: String): JsonNode {
        val stream = Resources.stream(classpathResource)

        return treeMapper.readTree(stream)
    }

    fun parse(source: Path): List<JsonNode> {
        val yamlParser = factory.createParser(source.toFile())
        return treeMapper
            .readValues(yamlParser, JsonNode::class.java)
            .readAll()
    }

    fun parseAsFile(content: String): List<JsonNode> {
        val yamlParser = factory.createParser(content)
        return treeMapper
            .readValues(yamlParser, JsonNode::class.java)
            .readAll()
    }

    fun parse(source: String): JsonNode {
        return treeMapper.readValue(source, JsonNode::class.java)
    }

    fun parseIfPossible(source: String?): JsonNode {
        source ?: return TextNode("")

        return try {
            parse(source)
        } catch (_: Exception) {
            TextNode(source)
        }
    }

    inline fun <reified T> parse(node: JsonNode): T {
        return mapper.treeToValue(node, T::class.java)
    }

    fun writeAsString(node: JsonNode): String {
        return treeMapper.writeValueAsString(node)
    }
}

fun JsonNode?.toDisplayYaml(): String {
    this ?: return ""
    if (isTextual) {
        return textValue()
    }
    return Yaml.writeAsString(this).trim()
}

