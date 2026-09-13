package specscript.language

import tools.jackson.databind.node.ObjectNode

interface Session {
    val name: String
    val data: ObjectNode
    fun close() {}
}

/**
 * Registry for named sessions, stored in ScriptContext.session. Open sessions form a stack:
 * the most recently opened session is the current one.
 */
class SessionRegistry<T : Session>(private val key: String, private val namePrefix: String) {

    init {
        registries.add(this)
    }

    private var currentSessionName: String? = null

    @Suppress("UNCHECKED_CAST")
    private fun ScriptContext.getSessions(): LinkedHashMap<String, T> =
        session.getOrPut(key) { LinkedHashMap<String, T>() } as LinkedHashMap<String, T>

    fun open(context: ScriptContext, session: T) {
        val sessions = context.getSessions()
        sessions.remove(session.name)?.close()
        sessions[session.name] = session
        currentSessionName = session.name
    }

    fun get(context: ScriptContext, name: String): T? = context.getSessions()[name]

    fun current(context: ScriptContext): T? {
        currentSessionName ?: return null

        return context.getSessions()[currentSessionName]
    }

    fun setCurrentSession(context: ScriptContext, name: String): T {
        if (context.getSessions().containsKey(name)) {
            currentSessionName = name
            return current(context)!!
        } else {
            throw SpecScriptCommandError("No session with name '$name' found.")
        }
    }

    fun close(context: ScriptContext, name: String) {
        context.getSessions().remove(name)?.close()
        if (name == currentSessionName) {
            currentSessionName = context.getSessions().values.lastOrNull()?.name
        }
    }

    fun closeAll(context: ScriptContext) {
        val sessions = context.getSessions()
        sessions.values.forEach { runCatching { it.close() } }
        sessions.clear()
        currentSessionName = null
    }

    fun generateName(context: ScriptContext): String {
        val counterKey = "$key.counter"
        val next = (context.session[counterKey] as? Int ?: 0) + 1
        context.session[counterKey] = next
        return "$namePrefix-%03d".format(next)
    }

    companion object {
        private val registries = mutableListOf<SessionRegistry<*>>()

        /**
         * Closes all sessions in all registries. Called when the top-level script run finishes.
         */
        fun closeAll(context: ScriptContext) {
            registries.forEach { it.closeAll(context) }
        }
    }
}
