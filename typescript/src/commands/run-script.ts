/**
 * Run script command: execute another .spec.yaml/.spec.md file as a sub-script.
 */

import { readFileSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import { DefaultContext, setRunScriptFileFn } from '../language/context.js'
import type { JsonValue } from '../language/types.js'
import { isObject, isString, isArray, CommandFormatError } from '../language/types.js'
import { Script } from '../language/script.js'
import { scanMarkdown } from '../markdown/scanner.js'
import { splitMarkdownSections } from '../markdown/converter.js'
import { resolvePath } from './files.js'
import { setupSilentCapture } from '../language/stdout-capture.js'

export const RunScriptCommand: CommandHandler = {
  name: 'Run script',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isString(data)) {
      // Value form: filename relative to scriptDir (not workingDir!)
      const filePath = resolve(context.scriptDir, data)
      return runScriptFile(filePath, {}, context)
    }

    if (isObject(data)) {
      // Object form: { file?, resource?, input? }
      let filePath: string
      if ('file' in data && isString(data.file)) {
        filePath = resolve(context.workingDir, data.file)
      } else if ('resource' in data && isString(data.resource)) {
        filePath = resolve(context.scriptDir, data.resource)
      } else {
        throw new CommandFormatError("Run script: expected 'file' or 'resource' property")
      }

      const input = data.input ?? {}

      // Auto-iterate arrays (Kotlin does this via runCommand() auto-list)
      if (isArray(input)) {
        const results: (JsonValue | undefined)[] = []
        let hasResults = false
        for (const item of input) {
          const result = await runScriptFile(filePath, item, context)
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

      return runScriptFile(filePath, input, context)
    }

    throw new CommandFormatError('Run script: expected filename string or object')
  },
}

/**
 * Run a script file with the given input, creating a child context.
 * Exported for use by local file command resolution.
 */
export async function runScriptFile(filePath: string, input: JsonValue, parentContext: ScriptContext): Promise<JsonValue | undefined> {
  if (!existsSync(filePath)) {
    throw new CommandFormatError(`File not found: ${filePath}`)
  }

  const content = readFileSync(filePath, 'utf-8')

  // Create a child context with fresh variables but shared session
  const childContext = (parentContext as DefaultContext).createChildContext(filePath, input)

  // Copy stdout capture from parent
  const parentStdout = parentContext.session.get('stdout')
  if (parentStdout) {
    setupSilentCapture(childContext)
  }

  if (filePath.endsWith('.spec.md')) {
    return runMarkdownScript(content, childContext)
  } else {
    return runYamlScript(content, childContext)
  }
}

async function runYamlScript(content: string, context: ScriptContext): Promise<JsonValue | undefined> {
  const script = Script.fromString(content)
  return script.run(context)
}

async function runMarkdownScript(content: string, context: ScriptContext): Promise<JsonValue | undefined> {
  const blocks = scanMarkdown(content)
  const scripts = splitMarkdownSections(blocks)

  for (const script of scripts) {
    if (script.commands.length === 0) continue
    await script.run(context)
  }

  return context.output
}

// Register the runScriptFile function to break circular dependency with context.ts
setRunScriptFileFn(runScriptFile)
