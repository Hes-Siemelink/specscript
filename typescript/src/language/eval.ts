import {
  type JsonValue, type JsonObject,
  isObject, isArray,
} from './types.js'
import type { ScriptContext } from './context.js'
import { runCommand } from './command-execution.js'

/**
 * Evaluate /CommandName inline expressions in a JSON tree.
 *
 * Walks the tree, finds object keys starting with '/', strips the prefix,
 * executes the command, and replaces the node with the result.
 * Runs during resolve() phase, BEFORE variable substitution.
 */
export function evalExpressions(data: JsonValue, context: ScriptContext): JsonValue {
  if (isArray(data)) {
    return data.map(item => evalExpressions(item, context))
  }

  if (!isObject(data)) {
    return data
  }

  const entries = Object.entries(data)
  for (let i = 0; i < entries.length; i++) {
    const [key, value] = entries[i]
    const evaluatedValue = evalExpressions(value, context)
    data[key] = evaluatedValue

    if (key.startsWith('/')) {
      const commandName = key.substring(1)
      const handler = context.getCommandHandler(commandName)
      const result = runCommand(handler, evaluatedValue, context)
      return result ?? ''
    }
  }

  return data
}
