import {
  type JsonValue, type JsonObject, type Command,
  isObject, isArray,
  SpecScriptCommandError, Break,
} from './types.js'
import type { ScriptContext } from './context.js'
import type { CommandHandler } from './command-handler.js'
import { runCommand } from './command-execution.js'
import { parseYamlCommands } from '../util/yaml.js'

/**
 * A parsed script: a flat list of commands with optional metadata.
 */
export class Script {
  readonly commands: Command[]
  readonly title?: string

  constructor(commands: Command[], title?: string) {
    this.commands = commands
    this.title = title
  }

  /**
   * Parse a YAML string into a Script.
   * Supports multi-document YAML (--- separators) and duplicate keys.
   */
  static fromString(content: string): Script {
    const commands = parseYamlCommands(content)
    return new Script(commands)
  }

  /**
   * Parse a JSON value (object, array, or list of objects) into a Script.
   */
  static fromData(data: JsonValue): Script {
    return new Script(toCommandList(data))
  }

  /**
   * Run this script in the given context.
   * Returns the final output value (or undefined).
   */
  run(context: ScriptContext): JsonValue | undefined {
    try {
      this.runCommands(context)
    } catch (e) {
      if (e instanceof Break) {
        return e.output
      }
      throw e
    }
    return context.output
  }

  /**
   * Execute all commands in sequence.
   * Handles error state: non-ErrorHandler commands are skipped when context.error is set.
   */
  runCommands(context: ScriptContext): void {
    for (const command of this.commands) {
      const handler = context.getCommandHandler(command.name)

      // Skip non-error-handler commands when there's an unhandled error
      if (context.error && !handler.errorHandler) {
        continue
      }

      try {
        runCommand(handler, command.data, context)
      } catch (e) {
        if (e instanceof Break) throw e
        if (e instanceof SpecScriptCommandError) {
          context.error = e
          continue
        }
        throw e
      }
    }

    // If there's still an unhandled error after the loop, throw it
    if (context.error) {
      const error = context.error
      context.error = undefined
      throw error
    }
  }

  /**
   * Split this script into test cases at "Test case" command boundaries.
   * Returns an array of { name, script } pairs.
   */
  splitTestCases(): Array<{ name: string; script: Script }> {
    const testCases: Array<{ name: string; script: Script }> = []
    let currentName = 'default'
    let currentCommands: Command[] = []

    for (const command of this.commands) {
      if (command.name.toLowerCase() === 'test case') {
        if (currentCommands.length > 0) {
          testCases.push({ name: currentName, script: new Script(currentCommands) })
        }
        currentName = typeof command.data === 'string' ? command.data : 'unnamed'
        currentCommands = []
      } else {
        currentCommands.push(command)
      }
    }

    if (currentCommands.length > 0) {
      testCases.push({ name: currentName, script: new Script(currentCommands) })
    }

    return testCases
  }

  /**
   * Split this script into a test suite: before-all, tests, after-all.
   */
  splitTests(): TestSuite {
    let setup: Command[] = []
    let tests: Command[] = []
    let teardown: Command[] = []
    let section: 'setup' | 'tests' | 'teardown' = 'setup'

    for (const command of this.commands) {
      const name = command.name.toLowerCase()
      if (name === 'before all tests') {
        section = 'setup'
        setup = toCommandList(command.data)
      } else if (name === 'tests') {
        section = 'tests'
        tests = toCommandList(command.data)
      } else if (name === 'after all tests') {
        section = 'teardown'
        teardown = toCommandList(command.data)
      } else {
        // Commands outside of these sections go to the current section
        switch (section) {
          case 'setup': setup.push(command); break
          case 'tests': tests.push(command); break
          case 'teardown': teardown.push(command); break
        }
      }
    }

    return { setup: new Script(setup), tests: new Script(tests), teardown: new Script(teardown) }
  }
}

export interface TestSuite {
  setup: Script
  tests: Script
  teardown: Script
}

/**
 * Convert a JSON value to a flat list of commands.
 * Objects: each key-value pair becomes a command.
 * Arrays: each element is recursively converted.
 */
export function toCommandList(data: JsonValue): Command[] {
  if (isObject(data)) {
    return Object.entries(data).map(([name, value]) => ({ name, data: value }))
  }
  if (isArray(data)) {
    const commands: Command[] = []
    for (const item of data) {
      commands.push(...toCommandList(item))
    }
    return commands
  }
  return []
}
