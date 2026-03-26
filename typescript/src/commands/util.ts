import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue } from '../language/types.js'
import { isString, CommandFormatError, toDisplayString } from '../language/types.js'
import type { ScriptContext } from '../language/context.js'
import { toDisplayYaml, parseYaml } from '../util/yaml.js'

/**
 * Json: converts a value to a compact JSON string.
 */
export const Json: CommandHandler = {
  name: 'Json',
  handlesLists: true,

  execute(data: JsonValue, _context: ScriptContext): JsonValue {
    return JSON.stringify(data)
  },
}

/**
 * Text: converts a value to its YAML display string.
 */
export const Text: CommandHandler = {
  name: 'Text',
  handlesLists: true,

  execute(data: JsonValue, _context: ScriptContext): JsonValue {
    return toDisplayString(data)
  },
}

/**
 * Print Json: pretty-prints JSON to stdout. Returns null.
 */
export const PrintJson: CommandHandler = {
  name: 'Print Json',
  handlesLists: true,

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    const json = JSON.stringify(data, null, 2)
    const writer = context.session.get('stdout') as ((s: string) => void) | undefined
    if (writer) {
      writer(json)
    } else {
      console.log(json)
    }
    return undefined
  },
}

/**
 * Parse Yaml: parses a string as YAML/JSON.
 */
export const ParseYamlCommand: CommandHandler = {
  name: 'Parse Yaml',

  execute(data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    if (!isString(data)) {
      throw new CommandFormatError('Parse Yaml expects a string value')
    }
    try {
      return parseYaml(data)
    } catch {
      return data
    }
  },
}

/**
 * Base64 encode: encodes a string to Base64.
 */
export const Base64Encode: CommandHandler = {
  name: 'Base64 encode',

  execute(data: JsonValue, _context: ScriptContext): JsonValue {
    if (!isString(data)) {
      throw new CommandFormatError('Base64 encode expects a string value')
    }
    return Buffer.from(data).toString('base64')
  },
}

/**
 * Base64 decode: decodes a Base64 string.
 */
export const Base64Decode: CommandHandler = {
  name: 'Base64 decode',

  execute(data: JsonValue, _context: ScriptContext): JsonValue {
    if (!isString(data)) {
      throw new CommandFormatError('Base64 decode expects a string value')
    }
    return Buffer.from(data, 'base64').toString('utf-8')
  },
}

/**
 * Wait: sleeps for N seconds. Returns null.
 */
export const WaitCommand: CommandHandler = {
  name: 'Wait',

  execute(data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    if (typeof data !== 'number') {
      throw new CommandFormatError("Invalid value for 'Wait' command.")
    }
    const ms = data * 1000
    // Synchronous sleep using Atomics.wait on a SharedArrayBuffer
    const buf = new SharedArrayBuffer(4)
    const arr = new Int32Array(buf)
    Atomics.wait(arr, 0, 0, ms)
    return undefined
  },
}
