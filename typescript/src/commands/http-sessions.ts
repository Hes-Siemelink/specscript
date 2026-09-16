/**
 * Http session commands: Http session, Http close session.
 *
 * Mirrors Kotlin's commands/http/HttpSession.kt.
 */

import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import { SessionRegistry, type Session } from '../language/sessions.js'
import type { JsonObject, JsonValue } from '../language/types.js'
import { CommandFormatError, isObject, isString } from '../language/types.js'
import { withoutSecrets } from '../util/json.js'

export interface HttpSessionEntry extends Session {
    data: JsonObject
}

export const httpSessionRegistry = new SessionRegistry<HttpSessionEntry>('http.sessions', 'http-session')

export const HttpSessionCommand: CommandHandler = {
    name: 'Http session',
    async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
        if (isString(data)) {
            return switchToSession(data, context)
        }

        if (!isObject(data)) {
            throw new CommandFormatError('Http session: expected a session object or a session name')
        }

        const name = (data.name as string | undefined) ?? httpSessionRegistry.generateName(context)
        data.name = name

        httpSessionRegistry.open(context, { name, data })

        return withoutSecrets(data)
    },
}

/**
 * Value form: switch the current session to the named open session.
 * A blank name gets the current session; an unknown name returns an empty object. Mirrors Kotlin.
 */
function switchToSession(sessionName: string, context: ScriptContext): JsonObject {
    const current = httpSessionRegistry.current(context)?.data ?? {}

    if (sessionName.trim() === '') {
        return withoutSecrets(current)
    }

    const target = httpSessionRegistry.get(context, sessionName)
    if (!target) {
        return {}
    }

    httpSessionRegistry.setCurrentSession(context, sessionName)
    return withoutSecrets(target.data)
}

export const HttpCloseSessionCommand: CommandHandler = {
    name: 'Http close session',
    async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
        const name = sessionNameFrom(data)
        if (name === undefined) {
            throw new CommandFormatError('Http close session: expected a session name or a session object')
        }
        await httpSessionRegistry.close(context, name)
        return undefined
    },
}

/** The session name from a value (string) or a session object (has a name property). */
function sessionNameFrom(data: JsonValue): string | undefined {
    if (isString(data)) return data
    if (isObject(data) && typeof data.name === 'string') return data.name
    return undefined
}