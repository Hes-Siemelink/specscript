package specscript.language

interface Session {
    val name: String
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

    @Suppress("UNCHECKED_CAST")
    private fun all(context: ScriptContext): LinkedHashMap<String, T> =
        context.session.getOrPut(key) { LinkedHashMap<String, T>() } as LinkedHashMap<String, T>

    fun open(context: ScriptContext, session: T) {
        val sessions = all(context)
        sessions.remove(session.name)?.close()
        sessions[session.name] = session
    }

    fun get(context: ScriptContext, name: String): T? = all(context)[name]

    fun current(context: ScriptContext): T? = all(context).values.lastOrNull()

    fun close(context: ScriptContext, name: String) {
        all(context).remove(name)?.close()
    }

    fun closeAll(context: ScriptContext) {
        val sessions = all(context)
        sessions.values.forEach { runCatching { it.close() } }
        sessions.clear()
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
