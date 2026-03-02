package specscript.commands.db

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.util.Json
import specscript.util.Json.newArray
import specscript.util.Json.newObject
import specscript.util.toDomainObject
import java.sql.Connection
import java.sql.DriverManager

object SQLite : CommandHandler("SQLite", "core/db"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val dataWithDefaults = SQLiteDefaults.getFrom(context)?.deepCopy<ObjectNode>()?.setAll(data) ?: data
        val sql = dataWithDefaults.toDomainObject(SQLiteData::class)

        // FIXME Use prepared statements to avoid SQL injection
        DriverManager.getConnection("jdbc:sqlite:${sql.file}").use { connection ->
            sql.update.forEach {
                connection.doUpdate(it)
            }

            val result = sql.query?.let {
                connection.doQuery(it).toNode()
            }

            return result
        }
    }
}

fun Connection.doUpdate(update: String) {
    this.createStatement().use { statement ->
        statement.executeUpdate(update)
    }
}

fun Connection.doQuery(query: String): List<Map<String, Any>> {
    val results = mutableListOf<Map<String, Any>>()
    this.createStatement().use { statement ->
        statement.executeQuery(query).use { resultSet ->
            while (resultSet.next()) {
                val row = mutableMapOf<String, Any>()
                for (i in 1..resultSet.metaData.columnCount) {
                    row[resultSet.metaData.getColumnName(i)] = resultSet.getObject(i)
                }
                results.add(row)
            }
        }
    }
    return results
}

fun List<Map<String, Any>>.toNode(): JsonNode {
    val node = newArray()
    this.forEach { row ->
        val rowNode = newObject()
        row.forEach { (key, value) ->
            rowNode.set<JsonNode>(key, value.toJsonNode())
        }
        node.add(rowNode)
    }
    return node
}

/** Convert a JDBC value to a Jackson JsonNode without YAML parsing. */
private fun Any?.toJsonNode(): JsonNode = when (this) {
    null -> TextNode("")
    is Number -> Json.mapper.valueToTree(this)
    is Boolean -> Json.mapper.valueToTree(this)
    is String -> jsonOrText(this)
    else -> TextNode(toString())
}

/** Try JSON first (for structured data stored as JSON strings), fall back to plain text. */
private fun jsonOrText(value: String): JsonNode = try {
    Json.mapper.readTree(value)
} catch (_: Exception) {
    TextNode(value)
}