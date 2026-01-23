package specscript.util

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.dataformat.yaml.YAMLWriteFeature
import tools.jackson.module.kotlin.kotlinModule
import java.nio.file.Path


object Yaml {

    val factory: YAMLFactory = YAMLFactory()
    val mapper: YAMLMapper = YAMLMapper.builder()
        .addModule(kotlinModule())
        .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
        .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
        .build()

//    init {
//        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
//    }

    fun readFile(source: Path): JsonNode {
        return mapper.readValue(source.toFile(), JsonNode::class.java)
    }

    fun readResource(classpathResource: String): JsonNode {
        val stream = Resources.stream(classpathResource)

        return mapper.readTree(stream)
    }

    fun parse(source: Path): List<JsonNode> {
        val yamlParser = factory.createParser(source.toFile())
        return mapper
            .readValues(yamlParser, JsonNode::class.java)
            .readAll()
    }

    fun parseAsFile(content: String): List<JsonNode> {
        val yamlParser = factory.createParser(content)
        return mapper
            .readValues(yamlParser, JsonNode::class.java)
            .readAll()
    }

    fun parse(source: String): JsonNode {
        return mapper.readValue(source, JsonNode::class.java)
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
}

fun JsonNode?.toDisplayYaml(): String {
    this ?: return ""
    if (isTextual) {
        return textValue()
    }
    return Yaml.mapper.writeValueAsString(this).trim()
}
