package specscript.commands.mcp

import specscript.language.ScriptContext
import specscript.language.SpecScriptCommandError

sealed interface McpTarget {
    data class Session(val session: McpSessionData) : McpTarget
    data object ConnectPerCall : McpTarget
}

/**
 * Resolves where an Mcp command should run. Resolution order: explicit `session` name →
 * explicit `server` (connect-per-call) → current open session → error.
 */
fun resolveMcpTarget(
    sessionName: String?,
    serverGiven: Boolean,
    commandName: String,
    context: ScriptContext,
): McpTarget {
    if (sessionName != null && serverGiven) {
        throw SpecScriptCommandError("Give either 'session' or 'server' on $commandName, not both")
    }

    return when {
        sessionName != null -> {
            val session = McpSession.sessions.get(context, sessionName)
                ?: throw SpecScriptCommandError("No open Mcp session: $sessionName")
            McpTarget.Session(session)
        }

        serverGiven -> McpTarget.ConnectPerCall

        else -> {
            val current = McpSession.sessions.current(context)
                ?: throw SpecScriptCommandError("No MCP server specified and no open Mcp session")
            McpTarget.Session(current)
        }
    }
}