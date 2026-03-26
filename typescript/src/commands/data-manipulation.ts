import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import {
  isObject, isArray, isString, isNumber,
  SpecScriptError, SpecScriptCommandError, CommandFormatError,
  toDisplayString,
} from '../language/types.js'
import type { ScriptContext } from '../language/context.js'

// --- Add ---

/**
 * Polymorphic addition: array concat, object merge, string concat, number addition.
 * Exported so AddTo and Append can reuse it.
 */
export function add(target: JsonValue, item: JsonValue): JsonValue {
  if (isArray(target)) {
    if (isArray(item)) {
      return [...target, ...item]
    }
    return [...target, item]
  }
  if (isObject(target)) {
    if (!isObject(item)) {
      throw new SpecScriptError(`Can't add a ${typeof item} to an object`)
    }
    return { ...target, ...item }
  }
  if (isString(target)) {
    if (isString(item) || isNumber(item) || typeof item === 'boolean' || item === null) {
      return target + toDisplayString(item)
    }
    throw new SpecScriptError(`Can't add a ${typeof item} to a string`)
  }
  if (isNumber(target)) {
    if (!isNumber(item)) {
      throw new SpecScriptError(`Can't add a ${typeof item} to a number`)
    }
    return target + item
  }
  throw new SpecScriptError(`Can't add to ${typeof target}`)
}

/**
 * Add: reduces an array of items using polymorphic addition.
 */
export const Add: CommandHandler = {
  name: 'Add',
  handlesLists: true,

  execute(data: JsonValue, _context: ScriptContext): JsonValue {
    if (!isArray(data)) {
      // Single item — return as-is
      return data
    }
    if (data.length === 0) {
      throw new CommandFormatError('Add requires at least one item')
    }
    let total = data[0]
    for (let i = 1; i < data.length; i++) {
      total = add(total, data[i])
    }
    return total
  },
}

// --- Add to ---

const VARIABLE_REGEX = /^\$\{(.+)}$/

/**
 * Wraps a JsonValue into an array if it isn't one already.
 */
function toArray(data: JsonValue): JsonValue[] {
  if (isArray(data)) return data
  return [data]
}

/**
 * Add to: mutates named variables by adding items.
 */
export const AddTo: CommandHandler = {
  name: 'Add to',

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    if (!isObject(data)) {
      throw new CommandFormatError('Add to expects an object')
    }
    for (const [key, value] of Object.entries(data)) {
      const match = VARIABLE_REGEX.exec(key)
      if (!match) {
        throw new CommandFormatError("Entries should be in ${..} variable syntax.")
      }
      const varName = match[1]
      let total = context.variables.get(varName)
      if (total === undefined) {
        throw new SpecScriptError(`Variable ${varName} not found.`)
      }
      for (const item of toArray(value)) {
        total = add(total, item)
      }
      context.variables.set(varName, total)
    }
    return undefined
  },
}

// --- Append ---

/**
 * Append: adds items to the current output using Add.add().
 */
export const Append: CommandHandler = {
  name: 'Append',
  handlesLists: true,

  execute(data: JsonValue, context: ScriptContext): JsonValue {
    let total = context.output
    if (total === undefined) return data

    for (const item of toArray(data)) {
      total = add(total, item)
    }
    return total
  },
}

// --- Fields ---

/**
 * Fields: returns array of keys from an object.
 */
export const Fields: CommandHandler = {
  name: 'Fields',

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    if (isObject(data)) {
      return Object.keys(data)
    }
    // ValueHandler path: use context.output
    const output = context.output
    if (output !== undefined && isObject(output)) {
      return Object.keys(output)
    }
    return undefined
  },
}

// --- Values ---

/**
 * Values: returns array of values from an object.
 */
export const Values: CommandHandler = {
  name: 'Values',

  execute(data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    if (!isObject(data)) {
      throw new CommandFormatError('Values expects an object')
    }
    return Object.values(data)
  },
}

// --- Size ---

/**
 * Size: returns the size/length of a value.
 */
export const Size: CommandHandler = {
  name: 'Size',
  handlesLists: true,

  execute(data: JsonValue, _context: ScriptContext): JsonValue {
    if (isArray(data)) return data.length
    if (isObject(data)) return Object.keys(data).length
    if (isString(data)) return data.length
    if (isNumber(data)) return data
    if (typeof data === 'boolean') return data ? 1 : 0
    throw new CommandFormatError(`Unsupported type for Size: ${typeof data}`)
  },
}

// --- Sort ---

/**
 * Sort: sorts an array of objects by a single field, ascending.
 */
export const Sort: CommandHandler = {
  name: 'Sort',

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    if (!isObject(data)) {
      throw new CommandFormatError('Sort expects an object with items and by')
    }
    const items = data['items'] ?? context.output
    if (items === undefined) {
      throw new CommandFormatError("Specify 'items' or make sure ${output} is set.")
    }
    if (!isArray(items)) {
      throw new CommandFormatError('items should be an array')
    }
    const sortField = data['by']
    if (!isString(sortField)) {
      throw new CommandFormatError("Sort requires a 'by' field name")
    }

    const sorted = [...items].sort((a, b) => {
      const va = isObject(a) ? a[sortField] : undefined
      const vb = isObject(b) ? b[sortField] : undefined
      if (va === undefined || vb === undefined) return 0
      if (isNumber(va) && isNumber(vb)) return va - vb
      return String(va).localeCompare(String(vb))
    })
    return sorted
  },
}

// --- Find ---

/**
 * Convert a SpecScript path to JSON Pointer segments.
 */
function pathToSegments(path: string): string[] {
  const segments: string[] = []
  let current = ''
  let i = 0
  while (i < path.length) {
    if (path[i] === '.') {
      if (current) segments.push(current)
      current = ''
      i++
    } else if (path[i] === '[') {
      if (current) {
        segments.push(current)
        current = ''
      }
      if (i + 1 < path.length && path[i + 1] === '"') {
        const closeQuote = path.indexOf('"', i + 2)
        segments.push(path.substring(i + 2, closeQuote))
        i = closeQuote + 2
      } else {
        const end = path.indexOf(']', i)
        segments.push(path.substring(i + 1, end))
        i = end + 1
      }
    } else {
      current += path[i]
      i++
    }
  }
  if (current) segments.push(current)
  return segments
}

/**
 * Navigate a JSON value using path segments.
 */
function navigateJsonPointer(value: JsonValue, segments: string[]): JsonValue | undefined {
  let current: JsonValue = value
  for (const seg of segments) {
    if (isArray(current)) {
      const idx = parseInt(seg, 10)
      if (isNaN(idx) || idx < 0 || idx >= current.length) return undefined
      current = current[idx]
    } else if (isObject(current)) {
      if (!(seg in current)) return undefined
      current = current[seg]
    } else {
      return undefined
    }
  }
  return current
}

/**
 * Find: navigates a JSON structure using a path.
 */
export const Find: CommandHandler = {
  name: 'Find',

  execute(data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    if (!isObject(data)) {
      throw new CommandFormatError('Find expects an object with path and in')
    }
    const path = data['path']
    if (!isString(path)) {
      throw new CommandFormatError("Find requires a 'path' string")
    }
    const source = data['in']
    if (source === undefined) {
      throw new CommandFormatError("Find requires an 'in' parameter")
    }
    const segments = pathToSegments(path)
    const result = navigateJsonPointer(source, segments)
    return result ?? null
  },
}

// --- Replace ---

/**
 * Recursive text find-and-replace.
 */
function replaceInValue(
  source: JsonValue,
  searchText: JsonValue,
  replacement: JsonValue
): JsonValue {
  if (isString(source)) {
    if (!isString(searchText)) {
      throw new CommandFormatError("'Replace.text' may contain text only")
    }
    const replacementText = toDisplayString(replacement)
    return source.split(searchText).join(replacementText)
  }
  if (isArray(source)) {
    return source.map(item => replaceInValue(item, searchText, replacement))
  }
  if (isObject(source)) {
    const result: JsonObject = {}
    for (const [key, value] of Object.entries(source)) {
      result[key] = replaceInValue(value, searchText, replacement)
    }
    return result
  }
  return source
}

/**
 * Replace: recursive text find-and-replace.
 */
export const Replace: CommandHandler = {
  name: 'Replace',

  execute(data: JsonValue, context: ScriptContext): JsonValue {
    if (!isObject(data)) {
      throw new CommandFormatError('Replace expects an object')
    }
    const text = data['text']
    if (text === undefined) {
      throw new CommandFormatError("Replace requires a 'text' parameter")
    }
    const source = data['in'] ?? context.output
    if (source === undefined) {
      throw new SpecScriptCommandError(
        "Replace needs 'in' parameter or non-null output variable."
      )
    }
    const replaceWith = data['with']
    if (replaceWith === undefined) {
      throw new CommandFormatError("Replace requires a 'with' parameter")
    }
    return replaceInValue(source, text, replaceWith)
  },
}
