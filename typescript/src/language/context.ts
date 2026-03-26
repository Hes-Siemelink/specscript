import { dirname, resolve } from 'node:path'
import { mkdtempSync, readdirSync, statSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import type { JsonValue, JsonObject } from './types.js'
import { SpecScriptCommandError, SpecScriptError } from './types.js'
import type { CommandHandler } from './command-handler.js'
import { getCommandHandler, canonicalName } from './command-handler.js'
import { createAssignment } from '../commands/variables.js'
import { parseYaml } from '../util/yaml.js'

const ASSIGNMENT_REGEX = /^\$\{(.+)}$/

/**
 * Script execution context.
 *
 * Holds variables, session state, error state, and command resolution.
 */
export interface ScriptContext {
  /** All variables in scope */
  variables: Map<string, JsonValue>

  /** Cross-script shared state (stdout capture, etc.) */
  session: Map<string, unknown>

  /** Path to the current script file */
  scriptFile: string

  /** Directory containing the current script */
  readonly scriptDir: string

  /** Working directory for file resolution */
  readonly workingDir: string

  /** Temp directory for the script session (lazy-created) */
  readonly tempDir: string

  /** Whether interactive prompts are allowed */
  interactive: boolean

  /** Current unhandled error, if any */
  error: SpecScriptCommandError | undefined

  /** Convenience getter/setter for variables.get('output') */
  get output(): JsonValue | undefined
  set output(value: JsonValue | undefined)

  /** Look up a command handler by name */
  getCommandHandler(name: string): CommandHandler

  /** Create a shallow copy for isolated execution */
  clone(): ScriptContext
}

/**
 * Default implementation of ScriptContext.
 */
export class DefaultContext implements ScriptContext {
  variables: Map<string, JsonValue>
  session: Map<string, unknown>
  scriptFile: string
  interactive: boolean
  error: SpecScriptCommandError | undefined

  private _scriptDir?: string
  private _workingDir?: string
  private _tempDir?: string
  private _commandResolver?: (name: string) => CommandHandler

  constructor(options?: {
    scriptFile?: string
    interactive?: boolean
    variables?: Map<string, JsonValue>
    session?: Map<string, unknown>
    commandResolver?: (name: string) => CommandHandler
    workingDir?: string
  }) {
    this.scriptFile = options?.scriptFile ?? '<inline>'
    this.interactive = options?.interactive ?? false
    this.variables = options?.variables ?? new Map()
    this.session = options?.session ?? new Map()
    this.error = undefined
    this._commandResolver = options?.commandResolver
    this._workingDir = options?.workingDir

    // Set built-in variables if not already present
    if (!this.variables.has('SCRIPT_HOME') && this.scriptFile !== '<inline>') {
      this.variables.set('SCRIPT_HOME', this.scriptDir)
    }
    if (!this.variables.has('env')) {
      const envObj: Record<string, JsonValue> = {}
      for (const [key, value] of Object.entries(process.env)) {
        if (value !== undefined) envObj[key] = value
      }
      this.variables.set('env', envObj)
    }
    if (!this.variables.has('input')) {
      this.variables.set('input', {})
    }
  }

  get scriptDir(): string {
    if (!this._scriptDir) {
      this._scriptDir = this.scriptFile === '<inline>'
        ? process.cwd()
        : dirname(resolve(this.scriptFile))
    }
    return this._scriptDir
  }

  get workingDir(): string {
    return this._workingDir ?? process.cwd()
  }

  get tempDir(): string {
    if (!this._tempDir) {
      // Check if already set in variables (shared via parent)
      const existing = this.variables.get('SCRIPT_TEMP_DIR')
      if (typeof existing === 'string') {
        this._tempDir = existing
      } else {
        this._tempDir = mkdtempSync(join(tmpdir(), 'specscript-'))
        this.variables.set('SCRIPT_TEMP_DIR', this._tempDir)
      }
    }
    return this._tempDir
  }

  get output(): JsonValue | undefined {
    return this.variables.get('output')
  }

  set output(value: JsonValue | undefined) {
    if (value !== undefined) {
      this.variables.set('output', value)
    } else {
      this.variables.delete('output')
    }
  }

  getCommandHandler(name: string): CommandHandler {
    // 1. Variable assignment syntax: ${varName}
    const assignMatch = ASSIGNMENT_REGEX.exec(name)
    if (assignMatch) {
      return createAssignment(assignMatch[1])
    }

    // 2. Custom resolver (for tests, file context, etc.)
    if (this._commandResolver) {
      try {
        return this._commandResolver(name)
      } catch {
        // Fall through to built-in registry
      }
    }

    // 3. Built-in command registry
    const handler = getCommandHandler(name)
    if (handler) {
      return handler
    }

    // 4. Local file commands (script files in the same directory)
    const localHandler = this.findLocalFileCommand(name)
    if (localHandler) {
      return localHandler
    }

    // 5. Imported file commands (from specscript-config.yaml)
    const importedHandler = this.findImportedCommand(name)
    if (importedHandler) {
      return importedHandler
    }

    throw new SpecScriptError(`Unknown command: ${name}`)
  }

  /**
   * Scan the script directory for .spec.yaml/.spec.md files that match the command name.
   */
  private findLocalFileCommand(name: string): CommandHandler | undefined {
    const canonical = canonicalName(name)
    try {
      const entries = readdirSync(this.scriptDir)
      for (const entry of entries) {
        const commandName = fileToCommandName(entry)
        if (commandName && canonicalName(commandName) === canonical) {
          const filePath = join(this.scriptDir, entry)
          return createFileCommandHandler(commandName, filePath)
        }
      }
    } catch {
      // Directory not readable — no local commands
    }
    return undefined
  }

  /**
   * Check specscript-config.yaml imports for matching commands.
   */
  private findImportedCommand(name: string): CommandHandler | undefined {
    const canonical = canonicalName(name)
    const configPath = join(this.scriptDir, 'specscript-config.yaml')
    try {
      const content = readFileSync(configPath, 'utf-8')
      const config = parseYaml(content) as JsonObject | null
      if (!config || !Array.isArray(config.imports)) return undefined

      for (const importPath of config.imports) {
        if (typeof importPath !== 'string') continue
        const fullPath = resolve(this.scriptDir, importPath)
        const fileName = importPath.split('/').pop() ?? ''
        const commandName = fileToCommandName(fileName)
        if (commandName && canonicalName(commandName) === canonical) {
          return createFileCommandHandler(commandName, fullPath)
        }
      }
    } catch {
      // No config file or not readable
    }
    return undefined
  }

  clone(): ScriptContext {
    const ctx = new DefaultContext({
      scriptFile: this.scriptFile,
      interactive: this.interactive,
      variables: new Map(this.variables),
      session: this.session, // shared, not cloned
      commandResolver: this._commandResolver,
      workingDir: this._workingDir,
    })
    ctx._scriptDir = this._scriptDir
    ctx._tempDir = this._tempDir
    ctx.error = this.error
    return ctx
  }

  /**
   * Create a child context for running a sub-script.
   * Shares session but gets fresh variables with input.
   */
  createChildContext(scriptFile: string, input: JsonValue): DefaultContext {
    const child = new DefaultContext({
      scriptFile,
      interactive: this.interactive,
      session: this.session, // shared
    })
    child.variables.set('input', input)
    return child
  }
}

/**
 * Convert a filename to a command name.
 * Strips .spec.yaml/.spec.md extension, replaces dashes with spaces, capitalizes first letter.
 */
export function fileToCommandName(filename: string): string | undefined {
  let base: string
  if (filename.endsWith('.spec.yaml')) {
    base = filename.slice(0, -'.spec.yaml'.length)
  } else if (filename.endsWith('.spec.md')) {
    base = filename.slice(0, -'.spec.md'.length)
  } else {
    return undefined
  }
  // Replace dashes with spaces
  base = base.replace(/-/g, ' ')
  // Capitalize first letter
  return base.charAt(0).toUpperCase() + base.slice(1)
}

// --- File command handler factory ---

/**
 * Registry for the runScriptFile function.
 * Set by run-script.ts at registration time to break the circular dependency.
 */
let _runScriptFileFn: ((filePath: string, input: JsonValue, parentContext: ScriptContext) => JsonValue | undefined) | undefined

export function setRunScriptFileFn(fn: (filePath: string, input: JsonValue, parentContext: ScriptContext) => JsonValue | undefined): void {
  _runScriptFileFn = fn
}

/**
 * Create a CommandHandler that runs a script file.
 * Used for local file commands and imported commands.
 */
function createFileCommandHandler(name: string, filePath: string): CommandHandler {
  return {
    name,
    execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
      if (!_runScriptFileFn) {
        throw new SpecScriptError('Run script command not registered. Register Level 3 commands before using local file commands.')
      }
      return _runScriptFileFn(filePath, data, context)
    },
  }
}
