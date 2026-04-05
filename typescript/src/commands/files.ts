/**
 * File commands: Temp file, Read file, Write file.
 */

import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs'
import { resolve, dirname, join } from 'node:path'
import { tmpdir } from 'node:os'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isString, CommandFormatError, SpecScriptCommandError } from '../language/types.js'
import { toDisplayYaml, parseYamlFile } from '../util/yaml.js'
import { resolve as resolveData } from '../language/command-execution.js'

// --- Path resolution helpers ---

/**
 * Resolve a file path from command data.
 * String → resolve against defaultDir.
 * Object with "file" → resolve against workingDir.
 * Object with "resource" → resolve against scriptDir.
 */
export function resolvePath(
  data: JsonValue,
  context: ScriptContext,
  defaultDir?: string
): string {
  if (isString(data)) {
    const dir = defaultDir ?? context.workingDir
    return resolve(dir, data)
  }
  if (isObject(data)) {
    if ('file' in data && isString(data.file)) {
      return resolve(context.workingDir, data.file)
    }
    if ('resource' in data && isString(data.resource)) {
      return resolve(context.scriptDir, data.resource)
    }
    throw new CommandFormatError("Expected either 'file' or 'resource' property.")
  }
  throw new CommandFormatError('Expected a filename string or object with file/resource.')
}

// --- Cd ---

export const CdCommand: CommandHandler = {
  name: 'Cd',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isString(data)) {
      throw new CommandFormatError('Cd expects a string value')
    }
    context.workingDir = data
    return undefined
  },
}

// --- Temp file ---

let tempFileCounter = 0

export const TempFileCommand: CommandHandler = {
  name: 'Temp file',
  delayedResolver: true,
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isString(data) || typeof data === 'number' || typeof data === 'boolean' || data === null) {
      // Value form: content as-is, resolve it first
      const resolved = await resolveData(data, context)
      const filePath = createTempFile(context)
      writeFileSync(filePath, toDisplayYaml(resolved))
      return filePath
    }

    if (isObject(data)) {
      const name = isString(data.name) ? data.name : undefined
      const shouldResolve = data.resolve !== false
      const contentRaw = 'content' in data ? data.content : null

      const content = shouldResolve ? await resolveData(contentRaw ?? null, context) : (contentRaw ?? null)
      const filePath = name
        ? createNamedTempFile(context, name)
        : createTempFile(context)
      writeFileSync(filePath, toDisplayYaml(content))
      return filePath
    }

    throw new CommandFormatError('Temp file: expected string content or object with content/name')
  },
}

function createTempFile(context: ScriptContext): string {
  const dir = context.tempDir
  const filePath = join(dir, `specscript-temp-file-${++tempFileCounter}`)
  return filePath
}

function createNamedTempFile(context: ScriptContext, filename: string): string {
  const filePath = join(context.tempDir, filename)
  const parentDir = dirname(filePath)
  if (!existsSync(parentDir)) {
    mkdirSync(parentDir, { recursive: true })
  }
  return filePath
}

// --- Read file ---

export const ReadFileCommand: CommandHandler = {
  name: 'Read file',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    const filePath = resolvePath(data, context)

    if (!existsSync(filePath)) {
      throw new CommandFormatError(`File not found: ${filePath}`)
    }

    const content = readFileSync(filePath, 'utf-8')
    const docs = parseYamlFile(content)

    if (docs.length === 0) return null
    if (docs.length === 1) return docs[0]
    return docs
  },
}

// --- Write file ---

export const WriteFileCommand: CommandHandler = {
  name: 'Write file',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    let filename: string
    let content: JsonValue

    if (isString(data)) {
      // Value form: filename, write context.output
      filename = data
      content = context.output ?? (() => {
        throw new SpecScriptCommandError(
          "Write file requires 'content' parameter or non-null output variable."
        )
      })()
    } else if (isObject(data)) {
      if (!('file' in data) || !isString(data.file)) {
        throw new CommandFormatError("Write file: missing required 'file' parameter")
      }
      filename = data.file
      content = 'content' in data ? data.content : (context.output ?? (() => {
        throw new SpecScriptCommandError(
          "Write file requires 'content' parameter or non-null output variable."
        )
      })())
    } else {
      throw new CommandFormatError('Write file: expected filename string or object with file/content')
    }

    // Resolve against workingDir
    const resolvedPath = resolve(context.workingDir, filename)

    // Create parent directories
    const parentDir = dirname(resolvedPath)
    if (parentDir && parentDir !== '.' && !existsSync(parentDir)) {
      mkdirSync(parentDir, { recursive: true })
    }

    writeFileSync(resolvedPath, toDisplayYaml(content))
    return undefined
  },
}
