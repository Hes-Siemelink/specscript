import type { JsonValue, JsonObject } from './types.js'
import type { ScriptContext } from './context.js'

/**
 * Command handler interface.
 *
 * Handlers declare what input types they accept and optional behavioral markers:
 * - delayedResolver: skip variable resolution before execution
 * - errorHandler: execute even when context.error is set
 * - handlesLists: if false and input is an array, runtime auto-iterates
 */
export interface CommandHandler {
  /** Command name (display name, e.g. "Print") */
  name: string

  /** If true, raw unresolved data is passed to execute() */
  delayedResolver?: boolean

  /** If true, this handler runs even when context has an unhandled error */
  errorHandler?: boolean

  /**
   * If true, arrays are passed directly to execute().
   * If false (default), arrays are auto-iterated with one execute() call per element.
   */
  handlesLists?: boolean

  /** Execute the command. Returns the result (stored as ${output}) or undefined. */
  execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined>
}

/**
 * Command registry — maps canonical (lowercased) command names to handlers.
 */
const registry = new Map<string, CommandHandler>()

export function registerCommand(handler: CommandHandler): void {
  registry.set(canonicalName(handler.name), handler)
}

export function getCommandHandler(name: string): CommandHandler | undefined {
  return registry.get(canonicalName(name))
}

export function canonicalName(name: string): string {
  return name.toLowerCase()
}

export function getAllCommands(): Map<string, CommandHandler> {
  return registry
}
