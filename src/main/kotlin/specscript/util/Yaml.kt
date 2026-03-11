package specscript.util

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.StringNode
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.dataformat.yaml.YAMLWriteFeature
import tools.jackson.module.kotlin.kotlinModule
import java.nio.file.Path


object Yaml {

    val factory: YAMLFactory = YAMLFactory()

    // Bare mapper for tree-model operations (parsing YAML to/from JsonNode).
    // No KotlinModule — avoids expensive kotlin-reflect initialization on startup.
    private val treeMapper: ObjectMapper = YAMLMapper.builder()
        .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
        .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
        .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .build()

    // Full mapper with KotlinModule, only for typed deserialization (treeToValue, readValue<T>, valueToTree).
    // Lazy to defer kotlin-reflect cost until first actual use.
    val mapper: ObjectMapper by lazy {
        YAMLMapper.builder()
            .addModule(kotlinModule())
            .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build()
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
        source ?: return StringNode("")

        return try {
            parse(source)
        } catch (_: Exception) {
            StringNode(source)
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
