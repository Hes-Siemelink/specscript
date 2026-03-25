import { dirname } from 'node:path'
import type { JsonValue } from './types.js'
import { SpecScriptCommandError, SpecScriptError } from './types.js'
import type { CommandHandler } from './command-handler.js'
import { getCommandHandler } from './command-handler.js'
import { createAssignment } from '../commands/variables.js'

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

  constructor(options?: {
    scriptFile?: string
    interactive?: boolean
    variables?: Map<string, JsonValue>
    session?: Map<string, unknown>
    commandResolver?: (name: string) => CommandHandler
  }) {
    this.scriptFile = options?.scriptFile ?? '<inline>'
    this.interactive = options?.interactive ?? false
    this.variables = options?.variables ?? new Map()
    this.session = options?.session ?? new Map()
    this.error = undefined
    this._commandResolver = options?.commandResolver

    // Set built-in variables if not already present
    if (!this.variables.has('SCRIPT_HOME') && this.scriptFile !== '<inline>') {
      this.variables.set('SCRIPT_HOME', dirname(this.scriptFile))
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

  private _commandResolver?: (name: string) => CommandHandler

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
      return this._commandResolver(name)
    }

    // 3. Built-in command registry
    const handler = getCommandHandler(name)
    if (handler) {
      return handler
    }

    throw new SpecScriptError(`Unknown command: ${name}`)
  }

  clone(): ScriptContext {
    const ctx = new DefaultContext({
      scriptFile: this.scriptFile,
      interactive: this.interactive,
      variables: new Map(this.variables),
      session: this.session, // shared, not cloned
      commandResolver: this._commandResolver,
    })
    ctx.error = this.error
    return ctx
  }
}
