import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { SpecScriptCommandError, isObject, CommandFormatError } from '../language/types.js'
import type { ScriptContext } from '../language/context.js'
import { Script } from '../language/script.js'

/**
 * Error: raises a user-handleable error.
 */
export const ErrorCommand: CommandHandler = {
  name: 'Error',

  async execute(data: JsonValue, _context: ScriptContext): Promise<JsonValue | undefined> {
    if (typeof data === 'string') {
      throw new SpecScriptCommandError(data)
    }
    if (isObject(data)) {
      const message = typeof data['message'] === 'string' ? data['message'] : 'Error'
      const type = data['type'] !== undefined && data['type'] !== null ? String(data['type']) : 'error'
      const errorData = data['data'] ?? undefined
      throw new SpecScriptCommandError(message, type, errorData)
    }
    throw new SpecScriptCommandError(String(data))
  },
}

// --- On error ---

/**
 * Convert a SpecScriptCommandError to a JSON object for the ${error} variable.
 */
function errorToJson(error: SpecScriptCommandError): JsonValue {
  const result: JsonObject = {
    type: error.type,
    message: error.message,
  }
  if (error.data !== undefined) {
    result['data'] = error.data
  }
  return result
}

/**
 * Run error handling: set ${error}, clear context.error, run handler, remove ${error}.
 * Exported for OnErrorType to reuse.
 */
export async function runErrorHandling(handlerBody: JsonValue, context: ScriptContext): Promise<void> {
  const error = context.error
  if (!error) return

  context.variables.set('error', errorToJson(error))
  context.error = undefined

  const script = Script.fromData(handlerBody)
  await script.run(context)

  context.variables.delete('error')
}

/**
 * On error: catches any error and runs the handler body.
 */
export const OnError: CommandHandler = {
  name: 'On error',
  delayedResolver: true,
  errorHandler: true,

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    await runErrorHandling(data, context)
    return undefined
  },
}

// --- On error type ---

/**
 * On error type: pattern-matches error type via object keys.
 */
export const OnErrorType: CommandHandler = {
  name: 'On error type',
  delayedResolver: true,
  errorHandler: true,

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('On error type expects an object')
    }

    for (const [key, value] of Object.entries(data)) {
      if (key === 'any' || key === context.error?.type) {
        await runErrorHandling(value, context)
        break
      }
    }

    return undefined
  },
}
