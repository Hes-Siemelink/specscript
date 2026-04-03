package specscript.commands.db

import specscript.language.CommandHandler
import specscript.language.DelayedResolver
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.resolve
import specscript.util.Json.newArray
import specscript.util.Json.newObject
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import java.sql.Connection
import java.sql.DriverManager

object SQLite : CommandHandler("SQLite", "core/db"), ObjectHandler, DelayedResolver {

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        // Extract raw SQL strings before variable resolution
        val updateNode = data.get("update")
        val rawUpdate = when (updateNode) {
            is ArrayNode -> updateNode.elements().asSequence().map { it.stringValue() }.toList()
            is StringNode -> listOf(updateNode.stringValue())
            else -> emptyList()
        }
        val rawQuery = data.get("query")?.stringValue()

        // Merge with defaults and resolve non-SQL fields
        val resolvedData = data.resolve(context) as ObjectNode
        val defaults = SQLiteDefaults.getFrom(context) as ObjectNode?
        val dataWithDefaults = defaults?.deepCopy()?.setAll(resolvedData) ?: resolvedData
        val file = dataWithDefaults.get("file")?.stringValue() ?: ""

        val dbPath = context.workingDir.resolve(file)
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            rawUpdate.forEach { sql ->
                connection.executePrepared(prepareSql(sql, context.variables))
            }

            return rawQuery?.let { sql ->
                connection.queryPrepared(prepareSql(sql, context.variables)).toNode()
            }
        }
    }
}

fun Connection.executePrepared(prepared: PreparedSql) {
    this.prepareStatement(prepared.sql).use { statement ->
        prepared.parameters.forEachIndexed { index, value ->
            statement.setObject(index + 1, value)
        }
        statement.executeUpdate()
    }
}

fun Connection.doUpdate(update: String) {
    this.createStatement().use { statement ->
        statement.executeUpdate(update)
    }
}

fun Connection.queryPrepared(prepared: PreparedSql): List<Map<String, Any>> {
    val results = mutableListOf<Map<String, Any>>()
    this.prepareStatement(prepared.sql).use { statement ->
        prepared.parameters.forEachIndexed { index, value ->
            statement.setObject(index + 1, value)
        }
        statement.executeQuery().use { resultSet ->
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
            rowNode.set(key, jdbcToJson(value))
        }
        node.add(rowNode)
    }
    return node
}

