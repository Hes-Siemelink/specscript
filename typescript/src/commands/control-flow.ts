import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import {
  Break, CommandFormatError,
  isObject, isArray, isString,
} from '../language/types.js'
import type { ScriptContext } from '../language/context.js'
import { Script } from '../language/script.js'
import { resolve } from '../language/command-execution.js'
import { toCondition } from '../language/conditions.js'
import { deepCopy } from '../language/types.js'

/**
 * Do: executes a sub-script (a block of commands).
 */
export const Do: CommandHandler = {
  name: 'Do',
  delayedResolver: true,
  handlesLists: true,

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    const script = Script.fromData(data)
    return script.runCommands(context)
  },
}

/**
 * Exit: terminates the current script and returns a value.
 */
export const Exit: CommandHandler = {
  name: 'Exit',

  async execute(data: JsonValue, _context: ScriptContext): Promise<JsonValue | undefined> {
    throw new Break(data)
  },
}

// --- If ---

/**
 * Evaluate an If condition block: extract then/else, resolve the condition,
 * and return the matching branch (unresolved) or null.
 * Exported for When to reuse.
 */
export async function evaluateIf(data: JsonObject, context: ScriptContext): Promise<JsonValue | undefined> {
  const workingCopy: JsonObject = { ...data }
  const thenBranch = workingCopy['then']
  if (thenBranch === undefined) {
    throw new CommandFormatError("Expected field 'then'.")
  }
  delete workingCopy['then']

  const elseBranch = workingCopy['else']
  delete workingCopy['else']

  // Resolve the condition part (everything except then/else)
  const resolvedCondition = await resolve(workingCopy, context)
  const condition = toCondition(resolvedCondition)

  return condition.isTrue() ? thenBranch : elseBranch
}

/**
 * Run a branch as a sub-script.
 */
async function runBranch(branch: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
  const script = Script.fromData(branch)
  return script.runCommands(context)
}

/**
 * If: conditional execution. DelayedResolver — resolves condition but not branches.
 */
export const If: CommandHandler = {
  name: 'If',
  delayedResolver: true,

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('If expects an object')
    }
    const branch = await evaluateIf(data, context)
    if (branch === undefined) return undefined
    return runBranch(branch, context)
  },
}

// --- When ---

/**
 * When: switch-like conditional. Takes an array of conditions, executes FIRST match.
 */
export const When: CommandHandler = {
  name: 'When',
  delayedResolver: true,
  handlesLists: true,

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isArray(data)) {
      throw new CommandFormatError('When expects an array')
    }

    for (const ifStatement of data) {
      if (!isObject(ifStatement)) {
        throw new CommandFormatError('Unsupported data type for When statement.')
      }

      // 'else' matches unconditionally — must be last
      if ('else' in ifStatement) {
        return runBranch(ifStatement['else'], context)
      }

      // Execute matching 'if' and exit
      const branch = await evaluateIf(ifStatement, context)
      if (branch !== undefined) {
        return runBranch(branch, context)
      }
    }

    return undefined
  },
}

// --- For each ---

const FOR_EACH_VARIABLE = /^\$\{(.+)} in$/

/**
 * Enumerate items for For each: arrays iterate directly,
 * objects produce {key, value} entries, scalars wrap in array.
 */
function enumerateForEach(items: JsonValue): JsonValue[] {
  if (isArray(items)) return items
  if (isObject(items)) {
    return Object.entries(items).map(([key, value]) => ({ key, value }))
  }
  // Scalar: wrap in array
  return [items]
}

/**
 * For each: loops over items, executing the body for each one.
 */
export const ForEach: CommandHandler = {
  name: 'For each',
  delayedResolver: true,

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue> {
    if (!isObject(data)) {
      throw new CommandFormatError('For each expects an object')
    }

    // Copy the data because we will modify it
    const body: JsonObject = { ...data }

    // Extract loop variable and items
    let loopVar = 'item'
    let itemData: JsonValue

    const firstKey = Object.keys(body)[0]
    const match = firstKey ? FOR_EACH_VARIABLE.exec(firstKey) : null

    if (match) {
      loopVar = match[1]
      itemData = body[firstKey]
      delete body[firstKey]
    } else {
      // No loop variable — use ${output}
      if (context.output === undefined) {
        throw new Error('For each without loop variable takes items from ${output}, but ${output} is null')
      }
      itemData = '${output}'
    }

    // Resolve the items (but not the body)
    const items = await resolve(itemData, context)
    const enumerated = enumerateForEach(items)

    // Determine output shape based on input shape
    const isObjectIteration = isObject(items)
    const results: JsonValue[] = []
    const objectResults: JsonObject = {}

    for (const item of enumerated) {
      // Set loop variable (leaks — matches Kotlin behavior)
      context.variables.set(loopVar, item)

      // Deep copy the body for each iteration (resolve is destructive)
      const bodyCopy = JSON.parse(JSON.stringify(body)) as JsonObject
      const script = Script.fromData(bodyCopy)
      const result = await script.runCommands(context)

      if (result !== undefined) {
        if (isObjectIteration && isObject(item)) {
          const key = isString(item['key']) ? item['key'] : String(item['key'])
          objectResults[key] = result
        } else {
          results.push(result)
        }
      }
    }

    return isObjectIteration ? objectResults : results
  },
}

// --- Repeat ---

/**
 * Repeat: loops until a condition is met.
 */
export const Repeat: CommandHandler = {
  name: 'Repeat',
  delayedResolver: true,

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Repeat expects an object')
    }

    const until = data['until']
    if (until === undefined) {
      throw new CommandFormatError("Repeat needs 'until'")
    }

    // Remove 'until' from the body
    const body: JsonObject = { ...data }
    delete body['until']

    let finished = false
    while (!finished) {
      // Deep copy the body for each iteration
      const bodyCopy = JSON.parse(JSON.stringify(body)) as JsonObject
      const script = Script.fromData(bodyCopy)
      const result = (await script.runCommands(context)) ?? context.output

      if (isObject(until)) {
        // Condition-based until
        const resolvedCondition = await resolve(deepCopy(until), context)
        const condition = toCondition(resolvedCondition)
        finished = condition.isTrue()
      } else {
        // Simple value comparison
        const resolvedUntil = await resolve(deepCopy(until), context)
        finished = (result === resolvedUntil)
      }
    }

    return undefined
  },
}
