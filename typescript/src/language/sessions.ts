import type { ScriptContext } from './context.js'

/**
 * A named session kept alive across commands. `close` is an optional lifecycle
 * hook: Mcp sessions close their live client, Http sessions need no action.
 * Mirrors Kotlin's `Session` interface in language/Sessions.kt (whose `close`
 * has a default no-op implementation).
 */
export interface Session {
    name: string
    close?(): void | Promise<void>
}

/**
 * Registry for named sessions, stored in ScriptContext.session.
 * Tracks the current session by name: most recent open picks it, and it can be
 * switched explicitly. Mirrors Kotlin's `SessionRegistry` in language/Sessions.kt.
 */
export class SessionRegistry<T extends Session> {
    private readonly key: string
    private readonly namePrefix: string
    private currentSessionName: string | undefined

    constructor(key: string, namePrefix: string) {
        this.key = key
        this.namePrefix = namePrefix
        this.currentSessionName = undefined
        registries.push(this as SessionRegistry<Session>)
    }

    private all(context: ScriptContext): Map<string, T> {
        let sessions = context.session.get(this.key) as Map<string, T> | undefined
        if (!sessions) {
            sessions = new Map()
            context.session.set(this.key, sessions)
        }
        return sessions
    }

    /** Open a session, replacing and closing any session with the same name. */
    open(context: ScriptContext, session: T): void {
        const sessions = this.all(context)
        const existing = sessions.get(session.name)
        if (existing) {
            void existing.close?.()
        }
        sessions.delete(session.name)
        sessions.set(session.name, session)
        this.currentSessionName = session.name
    }

    get(context: ScriptContext, name: string): T | undefined {
        return this.all(context).get(name)
    }

    /** The current session; undefined when none is open or the tracked name is gone. */
    current(context: ScriptContext): T | undefined {
        if (this.currentSessionName === undefined) return undefined
        return this.all(context).get(this.currentSessionName)
    }

    /** Switch the current session to the named open session. Throws when no such session exists. */
    setCurrentSession(context: ScriptContext, name: string): T {
        const session = this.all(context).get(name)
        if (!session) {
            throw new Error(`No session with name '${name}' found.`)
        }
        this.currentSessionName = name
        return session
    }

    /** Close a session by name. Silently no-ops when the session does not exist. */
    async close(context: ScriptContext, name: string): Promise<void> {
        const sessions = this.all(context)
        const session = sessions.get(name)
        if (session) {
            sessions.delete(name)
            await session.close?.()
        }
        if (name === this.currentSessionName) {
            const values = [...sessions.values()]
            this.currentSessionName = values.length > 0 ? values[values.length - 1].name : undefined
        }
    }

    /** Close all open sessions. Best effort — a failing close does not break the run. */
    async closeAll(context: ScriptContext): Promise<void> {
        const sessions = this.all(context)
        const closing = [...sessions.values()]
        sessions.clear()
        for (const session of closing) {
            try {
                await session.close?.()
            } catch {
                // Ignore close failures during cleanup
            }
        }
        this.currentSessionName = undefined
    }

    /** Generate a unique session name using a per-registry counter. */
    generateName(context: ScriptContext): string {
        const counterKey = `${this.key}.counter`
        const next = ((context.session.get(counterKey) as number | undefined) ?? 0) + 1
        context.session.set(counterKey, next)
        return `${this.namePrefix}-${String(next).padStart(3, '0')}`
    }
}

const registries: SessionRegistry<Session>[] = []

/**
 * Close all open sessions in all registries.
 * Called when the top-level script run finishes.
 * Mirrors Kotlin's SessionRegistry.closeAll companion function.
 */
export async function closeAllSessions(context: ScriptContext): Promise<void> {
    for (const registry of registries) {
        await registry.closeAll(context)
    }
}