/**
 * Run command: execute another .spec.yaml/.spec.md file or inline script.
 */

import { readFileSync, existsSync } from 'node:fs'
import { resolve as resolvePath } from 'node:path'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import { DefaultContext, setRunFileFn } from '../language/context.js'
import type { JsonValue } from '../language/types.js'
import { isObject, isString, isArray, CommandFormatError } from '../language/types.js'
import { Script } from '../language/script.js'
import { scanMarkdown } from '../markdown/scanner.js'
import { splitMarkdownSections } from '../markdown/converter.js'
import { propagateConnectionOverrides } from './connect-to.js'
import { resolve as resolveData } from '../language/command-execution.js'

export const RunCommand: CommandHandler = {
  name: 'Run',
  delayedResolver: true,
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isString(data)) {
      // Value form: resolve the string, then run relative to scriptDir
      const resolved = await resolveData(data, context) as string
      const filePath = resolvePath(context.scriptDir, resolved)
      return runScriptFile(filePath, {}, context)
    }

    if (isObject(data)) {
      // Selectively resolve metadata fields — NOT the inline script body
      const cd = data.cd ? await resolveData(data.cd, context) as string : undefined
      const cdPath = cd ? resolvePath(cd) : undefined
      const scriptNode = data.script
      const fileNode = data.file

      if (fileNode !== undefined) {
        // file: resolves relative to workingDir (or cd if set)
        const baseDir = cdPath ?? context.workingDir
        const resolvedFile = await resolveData(fileNode, context) as string
        const filePath = resolvePath(baseDir, resolvedFile)
        const input = data.input ? await resolveData(data.input, context) : {}
        return runWithInput(filePath, input, context)
      }

      if (scriptNode !== undefined && isString(scriptNode)) {
        // script: string — resolves relative to scriptDir (or cd if set)
        const baseDir = cdPath ?? context.scriptDir
        const resolvedScript = await resolveData(scriptNode, context) as string
        const filePath = resolvePath(baseDir, resolvedScript)
        const input = data.input ? await resolveData(data.input, context) : {}
        return runWithInput(filePath, input, context)
      }

      if (scriptNode !== undefined) {
        // script: inline — do NOT resolve the script body; it runs in its own context
        return runInlineScript(scriptNode, cdPath, context)
      }

      throw new CommandFormatError("Run: expected 'script' or 'file' property")
    }

    throw new CommandFormatError('Run: expected filename string or object')
  },
}

/**
 * Run a script file with input, handling auto-list iteration.
 */
async function runWithInput(filePath: string, input: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
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

  // Propagate parent's connection definitions for inheritance (first one wins)
  propagateConnectionOverrides(parentContext.scriptDir, childContext)

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

/**
 * Run an inline script in a child context with variable isolation.
 */
async function runInlineScript(scriptNode: JsonValue, cdPath: string | undefined, context: ScriptContext): Promise<JsonValue | undefined> {
  const scriptDir = cdPath ?? context.scriptDir
  const workingDir = cdPath ?? context.workingDir

  const childContext = new DefaultContext({
    scriptFile: scriptDir,
    interactive: context.interactive,
    session: context.session as Map<string, unknown>,
    workingDir,
  })
  // Fresh variables — no parent variables leak in
  childContext.variables = new Map()
  childContext.variables.set('SCRIPT_HOME', scriptDir)
  childContext.variables.set('PWD', process.cwd())
  const envObj: Record<string, JsonValue> = {}
  for (const [key, value] of Object.entries(process.env)) {
    if (value !== undefined) envObj[key] = value
  }
  childContext.variables.set('env', envObj)
  childContext.variables.set('input', {})

  // Delegate command lookup to the parent context so inline scripts
  // can use the host script's local commands, imports, and connections
  if (context instanceof DefaultContext) {
    childContext.parentCommandLookup = context
  }

  const script = Script.fromData(scriptNode)
  return script.run(childContext)
}

// Register the runScriptFile function to break circular dependency with context.ts
setRunFileFn(runScriptFile)
