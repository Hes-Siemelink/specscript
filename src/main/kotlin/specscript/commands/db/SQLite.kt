package specscript.commands.db

import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.util.Json.newArray
import specscript.util.Json.newObject
import specscript.util.Yaml
import specscript.util.toDomainObject
import specscript.util.toJsonNode
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import java.sql.Connection
import java.sql.DriverManager

object SQLite : CommandHandler("SQLite", "core/db"), ObjectHandler {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val dataWithDefaults = (SQLiteDefaults.getFrom(context) as ObjectNode?)?.deepCopy()?.setAll(data) ?: data
        val sql = dataWithDefaults.toDomainObject(SQLiteData::class)

        // FIXME Use prepared statements to avoid SQL injection
        val dbPath = context.workingDir.resolve(sql.file)
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
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
            rowNode.set(key, jdbcToJsonNode(value))
        }
        node.add(rowNode)
    }
    return node
}

/** Convert a JDBC value to a Jackson JsonNode without YAML parsing. */
private fun jdbcToJsonNode(value: Any?): JsonNode = when (value) {
    null -> StringNode("")
    is Number -> value.toJsonNode()
    is Boolean -> BooleanNode.valueOf(value)
    is String -> Yaml.parseIfPossible(value)
    else -> StringNode(value.toString())
}
