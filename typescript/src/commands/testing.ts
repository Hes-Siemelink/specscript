import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import {
  isObject, isArray, isString,
  SpecScriptCommandError, MissingExpectedError,
  CommandFormatError,
} from '../language/types.js'
import type { ScriptContext } from '../language/context.js'
import { deepEquals, toCondition } from '../language/conditions.js'
import { toDisplayYaml } from '../util/yaml.js'

/**
 * Assert equals: asserts that two values are structurally equal.
 */
export const AssertEquals: CommandHandler = {
  name: 'Assert equals',

  execute(data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    if (!isObject(data)) {
      throw new CommandFormatError('Assert equals requires an object with "actual" and "expected"')
    }
    const actual = data['actual']
    const expected = data['expected']
    if (!deepEquals(actual, expected)) {
      throw new SpecScriptCommandError(
        `Assertion failed:\nExpected: ${toDisplayYaml(expected)}\nActual:   ${toDisplayYaml(actual)}`,
        'assertion-error'
      )
    }
    return undefined
  },
}

/**
 * Assert that: asserts that a condition is true.
 */
export const AssertThat: CommandHandler = {
  name: 'Assert that',

  execute(data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    if (!isObject(data)) {
      throw new CommandFormatError('Assert that requires a condition object')
    }
    const condition = toCondition(data)
    if (!condition.isTrue()) {
      throw new SpecScriptCommandError(
        `Assertion failed: condition is false\n${toDisplayYaml(data)}`,
        'assertion-error'
      )
    }
    return undefined
  },
}

/**
 * Expected output: asserts that the current output matches the expected value.
 */
export const ExpectedOutput: CommandHandler = {
  name: 'Expected output',
  handlesLists: true,

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    const actual = context.output
    if (actual === undefined && data === null) return undefined
    if (!deepEquals(actual ?? null, data)) {
      throw new SpecScriptCommandError(
        `Expected output:\n${toDisplayYaml(data)}\nActual output:\n${toDisplayYaml(actual ?? null)}`,
        'assertion-error'
      )
    }
    return undefined
  },
}

/**
 * Expected console output: asserts that the captured console output matches.
 */
export const ExpectedConsoleOutput: CommandHandler = {
  name: 'Expected console output',

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    const captured = context.session.get('capturedOutput') as string[] | undefined
    const actualLines = captured ?? []
    const actual = actualLines.join('\n')

    const expected = typeof data === 'string' ? data : toDisplayYaml(data)

    if (actual !== expected) {
      throw new SpecScriptCommandError(
        `Expected console output:\n${expected}\nActual console output:\n${actual}`,
        'assertion-error'
      )
    }
    // Clear captured output after assertion
    if (captured) {
      captured.length = 0
    }
    return undefined
  },
}

/**
 * Expected error: asserts that an error occurred and clears it.
 */
export const ExpectedError: CommandHandler = {
  name: 'Expected error',
  errorHandler: true,

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    const error = context.error
    if (!error) {
      throw new MissingExpectedError('Expected an error but none occurred')
    }

    // If data specifies what to check, validate the error
    if (isString(data)) {
      // data is the expected error message
      if (error.message !== data) {
        throw new SpecScriptCommandError(
          `Expected error message: ${data}\nActual error message: ${error.message}`,
          'assertion-error'
        )
      }
    } else if (isObject(data)) {
      if ('message' in data && error.message !== data['message']) {
        throw new SpecScriptCommandError(
          `Expected error message: ${data['message']}\nActual error message: ${error.message}`,
          'assertion-error'
        )
      }
      if ('type' in data && error.type !== data['type']) {
        throw new SpecScriptCommandError(
          `Expected error type: ${data['type']}\nActual error type: ${error.type}`,
          'assertion-error'
        )
      }
    }

    // Clear the error
    context.error = undefined
    return undefined
  },
}

/**
 * Test case: names a test case boundary. No-op during execution.
 */
export const TestCase: CommandHandler = {
  name: 'Test case',

  execute(_data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    return undefined
  },
}

/**
 * Code example: names a code example. Same as Test case — a boundary marker.
 */
export const CodeExample: CommandHandler = {
  name: 'Code example',

  execute(_data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    return undefined
  },
}

/**
 * Answers: pre-records answers for interactive prompts (test infrastructure).
 */
export const Answers: CommandHandler = {
  name: 'Answers',

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    if (isObject(data)) {
      const existing = (context.session.get('answers') as Map<string, JsonValue>) ?? new Map<string, JsonValue>()
      for (const [key, value] of Object.entries(data)) {
        existing.set(key, value)
      }
      context.session.set('answers', existing)
    }
    return undefined
  },
}

/**
 * Tests: container for test commands. DelayedResolver — content is raw.
 * No-op during normal execution; used structurally by splitTests().
 */
export const Tests: CommandHandler = {
  name: 'Tests',
  delayedResolver: true,
  handlesLists: true,

  execute(_data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    return undefined
  },
}

/**
 * Before all tests: setup commands run once before test cases.
 * DelayedResolver — content is raw. No-op during normal execution.
 */
export const BeforeAllTests: CommandHandler = {
  name: 'Before all tests',
  delayedResolver: true,
  handlesLists: true,

  execute(_data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    return undefined
  },
}

/**
 * After all tests: teardown commands run once after test cases.
 * DelayedResolver — content is raw. No-op during normal execution.
 */
export const AfterAllTests: CommandHandler = {
  name: 'After all tests',
  delayedResolver: true,
  handlesLists: true,

  execute(_data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    return undefined
  },
}
