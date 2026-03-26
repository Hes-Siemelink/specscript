import {
  type JsonValue, type JsonObject,
  isObject, isArray,
  deepCopy,
  SpecScriptError, SpecScriptCommandError, SpecScriptInternalError,
  CommandFormatError, Break,
} from './types.js'
import type { CommandHandler } from './command-handler.js'
import type { ScriptContext } from './context.js'
import { resolveVariables } from './variables.js'
import { evalExpressions } from './eval.js'

/**
 * Run a single command through the full pipeline: resolve → dispatch.
 * Handles auto-list iteration for commands that don't handle arrays.
 */
export function runCommand(handler: CommandHandler, rawData: JsonValue, context: ScriptContext): JsonValue | undefined {
  // Auto-list: if data is an array and handler doesn't handle lists, iterate
  if (isArray(rawData) && !handler.handlesLists) {
    return runCommandOnList(handler, rawData, context)
  }
  return runSingleCommand(handler, rawData, context)
}

/**
 * Auto-iterate an array, calling the handler once per element.
 * Collects results into a new array.
 */
function runCommandOnList(handler: CommandHandler, list: JsonValue[], context: ScriptContext): JsonValue | undefined {
  const results: JsonValue[] = []
  let hasResults = false

  for (const item of list) {
    const result = runSingleCommand(handler, item, context)
    if (result !== undefined) {
      results.push(result)
      hasResults = true
    } else {
      results.push(null)
    }
  }

  if (!hasResults) return undefined
  const output = results as JsonValue
  context.output = output
  return output
}

/**
 * Execute a single command: resolve data, then dispatch to handler.
 */
function runSingleCommand(handler: CommandHandler, rawData: JsonValue, context: ScriptContext): JsonValue | undefined {
  // Resolve phase: eval + variable substitution (unless delayed)
  const data = handler.delayedResolver
    ? rawData
    : resolve(rawData, context)

  // Dispatch phase
  return handleCommand(handler, data, context)
}

/**
 * Resolve a JSON value: deep copy, eval inline commands, then substitute variables.
 * Exported so DelayedResolver commands can selectively resolve sub-expressions.
 */
export function resolve(data: JsonValue, context: ScriptContext): JsonValue {
  const copied = deepCopy(data)
  const evaluated = evalExpressions(copied, context)
  return resolveVariables(evaluated, context.variables)
}

/**
 * Dispatch to the handler with error wrapping.
 */
function handleCommand(handler: CommandHandler, data: JsonValue, context: ScriptContext): JsonValue | undefined {
  try {
    const result = handler.execute(data, context)
    if (result !== undefined) {
      context.output = result
    }
    return result
  } catch (e) {
    if (e instanceof Break) throw e
    if (e instanceof SpecScriptCommandError) throw e
    if (e instanceof SpecScriptError) {
      e.command = e.command ?? handler.name
      throw e
    }
    // Wrap unexpected errors
    const wrapped = new SpecScriptInternalError(
      `Error in command '${handler.name}': ${e instanceof Error ? e.message : String(e)}`,
      e instanceof Error ? e : undefined
    )
    wrapped.command = handler.name
    throw wrapped
  }
}
