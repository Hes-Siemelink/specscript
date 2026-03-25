import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue } from '../language/types.js'
import { SpecScriptCommandError, isObject } from '../language/types.js'
import type { ScriptContext } from '../language/context.js'

/**
 * Error: raises a user-handleable error.
 */
export const ErrorCommand: CommandHandler = {
  name: 'Error',

  execute(data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    if (typeof data === 'string') {
      throw new SpecScriptCommandError(data)
    }
    if (isObject(data)) {
      const message = typeof data['message'] === 'string' ? data['message'] : 'Error'
      const type = typeof data['type'] === 'string' ? data['type'] : 'error'
      const errorData = data['data'] ?? undefined
      throw new SpecScriptCommandError(message, type, errorData)
    }
    throw new SpecScriptCommandError(String(data))
  },
}
