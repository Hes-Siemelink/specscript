package specscript.commands.db

import specscript.language.CommandHandler
import specscript.language.ObjectHandler
import specscript.language.ScriptContext
import specscript.language.ValueHandler
import specscript.util.Json
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.ValueNode

object SQLiteDefaults : CommandHandler("SQLite defaults", "core/db"), ObjectHandler, ValueHandler {

    private const val SQLITE_DEFAULTS = "sqlite.defaults"

    private fun store(context: ScriptContext, value: JsonNode) {
        context.session[SQLITE_DEFAULTS] = value
    }

    fun getFrom(context: ScriptContext): JsonNode? {
        return context.session[SQLITE_DEFAULTS] as JsonNode?
    }

    override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
        return getFrom(context) ?: Json.newObject()
    }

    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        store(context, data)
        return null
    }
}