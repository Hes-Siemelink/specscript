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
export async function evalExpressions(data: JsonValue, context: ScriptContext): Promise<JsonValue> {
  if (isArray(data)) {
    const results: JsonValue[] = []
    for (const item of data) {
      results.push(await evalExpressions(item, context))
    }
    return results
  }

  if (!isObject(data)) {
    return data
  }

  const entries = Object.entries(data)
  for (let i = 0; i < entries.length; i++) {
    const [key, value] = entries[i]
    const evaluatedValue = await evalExpressions(value, context)
    data[key] = evaluatedValue

    if (key.startsWith('/')) {
      const commandName = key.substring(1)
      const handler = context.getCommandHandler(commandName)
      const result = await runCommand(handler, evaluatedValue, context)
      return result ?? ''
    }
  }

  return data
}
