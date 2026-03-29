package specscript.files

import specscript.language.SpecScriptException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

data class PackageImport(
    val source: String,
    val items: List<ImportItem>,
    val local: Boolean
) {

    companion object {

        fun parse(imports: JsonNode?): List<PackageImport> {
            if (imports == null || imports.isMissingNode || imports.isNull) return emptyList()

            if (imports is ArrayNode) {
                throw SpecScriptException(
                    "imports must be a map, not a list. See the Packages specification for the correct format."
                )
            }

            if (imports !is ObjectNode) throw SpecScriptException("imports must be a map")

            return imports.properties().map { (source, value) ->
                val local = source.startsWith("./")
                val items = ImportItem.parseList(value)
                PackageImport(source, items, local)
            }
        }
    }
}

sealed interface ImportItem {
    data class Command(val path: String, val name: String, val alias: String? = null) : ImportItem
    data class Name(val value: String, val alias: String? = null) : ImportItem
    data class Directory(val path: String) : ImportItem
    data class Wildcard(val path: String, val recursive: Boolean) : ImportItem

    companion object {

        fun parseList(node: JsonNode): List<ImportItem> {
            if (node.isNull) return listOf(Wildcard("", recursive = false))

            return when (node) {
                is StringNode -> listOf(parse(node.stringValue()))

                is ArrayNode -> node.elements().asSequence().map { parseElement(it) }.toList()

                else -> throw SpecScriptException("Import value must be a string or list")
            }
        }

        private fun parse(value: String): ImportItem {
            if (value == "*") return Wildcard("", recursive = false)
            if (value == "**") return Wildcard("", recursive = true)
            if (value.endsWith("/*")) return Wildcard(value.removeSuffix("/*"), recursive = false)
            if (value.endsWith("/**")) return Wildcard(value.removeSuffix("/**"), recursive = true)

            val slashIndex = value.lastIndexOf('/')
            if (slashIndex > 0) {
                val commandName = value.substring(slashIndex + 1)
                return Command(value, commandName)
            }

            return Name(value)
        }

        private fun parseElement(node: JsonNode): ImportItem {
            return when (node) {
                is StringNode -> parse(node.stringValue())

                is ObjectNode -> {
                    val (key, value) = node.properties().first()
                    val alias = (value as? ObjectNode)?.get("as")?.stringValue()
                    parseWithAlias(key, alias)
                }

                else -> throw SpecScriptException("Import item must be a string or map")
            }
        }

        private fun parseWithAlias(key: String, alias: String?): ImportItem {
            val slashIndex = key.lastIndexOf('/')
            if (slashIndex > 0) {
                val commandName = key.substring(slashIndex + 1)
                return Command(key, commandName, alias)
            }

            if (alias != null) {
                return Name(key, alias)
            }

            return Directory(key)
        }
    }
}
