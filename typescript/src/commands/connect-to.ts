/**
 * Connect to command: resolve a named connection and execute it.
 *
 * Connections are defined in specscript-config.yaml files. The command supports:
 * - Inline SpecScript commands (object value)
 * - Script file references (string value)
 * - Upward directory search (walks parent dirs to find matching connection)
 * - Connection inheritance (parent context connections take precedence)
 *
 * Mirrors Kotlin's ConnectTo.kt, FileContext connection propagation, and
 * SpecScriptDirectories.findConnection().
 */

import { existsSync, readFileSync } from 'node:fs'
import { join, resolve, dirname, basename } from 'node:path'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import { DefaultContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isString, isObject, SpecScriptError, CommandFormatError } from '../language/types.js'
import { parseYaml } from '../util/yaml.js'
import { Script } from '../language/script.js'

// --- Session keys ---
const CACHE_KEY = 'connect-to.cache'
const OVERRIDES_KEY = 'connect-to.overrides'

// --- Public API for connection override propagation ---

/**
 * Get the connection overrides map from the session.
 * Creates it if it doesn't exist.
 */
export function getConnectionOverrides(context: ScriptContext): Map<string, JsonValue> {
  let overrides = context.session.get(OVERRIDES_KEY) as Map<string, JsonValue> | undefined
  if (!overrides) {
    overrides = new Map<string, JsonValue>()
    context.session.set(OVERRIDES_KEY, overrides)
  }
  return overrides
}

/**
 * Propagate connection definitions from a parent context's directory config
 * into the session overrides. Uses putIfAbsent semantics (first one wins).
 */
export function propagateConnectionOverrides(parentDir: string, context: ScriptContext): void {
  const connections = readConnectionsFromConfig(parentDir)
  if (!connections) return

  const overrides = getConnectionOverrides(context)
  for (const [name, value] of Object.entries(connections)) {
    if (!overrides.has(name)) {
      overrides.set(name, value)
    }
  }
}

// --- Connection cache ---

function getCache(context: ScriptContext): Map<string, JsonValue | undefined> {
  let cache = context.session.get(CACHE_KEY) as Map<string, JsonValue | undefined> | undefined
  if (!cache) {
    cache = new Map()
    context.session.set(CACHE_KEY, cache)
  }
  return cache
}

// --- Config reading ---

function readConnectionsFromConfig(dir: string): JsonObject | undefined {
  const configPath = join(dir, 'specscript-config.yaml')
  try {
    const content = readFileSync(configPath, 'utf-8')
    const config = parseYaml(content)
    if (config && isObject(config) && isObject(config.connections)) {
      return config.connections as JsonObject
    }
  } catch {
    // No config or not readable
  }
  return undefined
}

// --- Upward directory search ---

interface ConnectionMatch {
  definition: JsonValue
  configDir: string
}

function findConnection(name: string, startDir: string): ConnectionMatch | undefined {
  let dir: string | undefined = resolve(startDir)
  while (dir) {
    const connections = readConnectionsFromConfig(dir)
    if (connections && name in connections) {
      return { definition: connections[name], configDir: dir }
    }
    const parent = dirname(dir)
    if (parent === dir) break
    dir = parent
  }
  return undefined
}

// --- Execute a connection definition ---

async function connect(
  definition: JsonValue,
  configDir: string,
  context: ScriptContext,
): Promise<JsonValue | undefined> {
  if (isString(definition)) {
    // File reference: resolve relative to the config directory, run as a sub-script
    const filePath = resolve(configDir, definition)
    if (!existsSync(filePath)) {
      throw new SpecScriptError(`Connection script not found: ${filePath}`)
    }
    const content = readFileSync(filePath, 'utf-8')
    const childContext = (context as DefaultContext).createChildContext(filePath, {})
    const script = Script.fromString(content)
    return script.run(childContext)
  }

  // Inline commands: run as inline SpecScript
  const script = Script.fromData(definition)
  return script.run(context)
}

// --- Command ---

export const ConnectToCommand: CommandHandler = {
  name: 'Connect to',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isString(data)) {
      throw new CommandFormatError('Connect to: expected a connection name string')
    }
    const target = data

    // Already connected — return cached result
    const cache = getCache(context)
    if (cache.has(target)) {
      return cache.get(target)
    }

    // Check inherited overrides (first one wins)
    const overrides = getConnectionOverrides(context)
    const override = overrides.get(target)
    if (override !== undefined) {
      const result = await connect(override, context.scriptDir, context)
      cache.set(target, result)
      return result
    }

    // Search config files upward from script directory
    const match = findConnection(target, context.scriptDir)
    if (!match) {
      throw new SpecScriptError(
        `No connection configured for ${target} in ${basename(context.scriptDir)} or any parent directory`
      )
    }

    const result = await connect(match.definition, match.configDir, context)
    cache.set(target, result)
    return result
  },
}
