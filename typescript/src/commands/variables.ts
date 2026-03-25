import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue } from '../language/types.js'
import type { ScriptContext } from '../language/context.js'

/**
 * Output: sets the output variable to the given value.
 */
export const Output: CommandHandler = {
  name: 'Output',
  handlesLists: true,

  execute(data: JsonValue, _context: ScriptContext): JsonValue {
    return data
  },
}

/**
 * As: assigns the current output to a named variable.
 */
export const As: CommandHandler = {
  name: 'As',
  delayedResolver: true,

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    if (typeof data !== 'string') return undefined
    // Strip ${...} wrapping if present
    const varName = data.replace(/^\$\{(.+)}$/, '$1')
    const output = context.output
    if (output !== undefined) {
      context.variables.set(varName, output)
    }
    return undefined
  },
}

/**
 * Assignment: assigns a value to a named variable.
 * This is the handler for "${varName}: value" syntax.
 */
export function createAssignment(varName: string): CommandHandler {
  return {
    name: `\${${varName}}`,
    handlesLists: true,

    execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
      context.variables.set(varName, data)
      return undefined
    },
  }
}
