import { readFileSync, existsSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { describe, it, beforeAll, afterAll, expect } from 'vitest'
import { Script, toCommandList } from '../src/language/script.js'
import { DefaultContext } from '../src/language/context.js'
import { registerLevel0Commands } from '../src/commands/register.js'
import { setupSilentCapture } from '../src/language/stdout-capture.js'
import type { JsonValue } from '../src/language/types.js'
import { isObject } from '../src/language/types.js'
import { parseYamlCommands } from '../src/util/yaml.js'

// Register commands once
registerLevel0Commands()

/**
 * Find the SpecScript home directory (repo root with specification/).
 */
function findSpecScriptHome(): string {
  if (process.env.SPECSCRIPT_HOME) return process.env.SPECSCRIPT_HOME
  let dir = dirname(new URL(import.meta.url).pathname)
  for (let i = 0; i < 10; i++) {
    const parent = dirname(dir)
    if (existsSync(join(parent, 'specification'))) return parent
    dir = parent
  }
  throw new Error('Cannot find SpecScript home (no specification/ directory found)')
}

const SPECSCRIPT_HOME = findSpecScriptHome()
const SPEC_DIR = join(SPECSCRIPT_HOME, 'specification')

/** Level 0 spec.yaml test files (relative to specification/) */
const LEVEL_0_TEST_FILES = [
  'language/tests/Variables tests.spec.yaml',
  'commands/core/testing/tests/Assert tests.spec.yaml',
  'commands/core/variables/tests/Assignment tests.spec.yaml',
  'commands/core/variables/tests/Output variable tests.spec.yaml',
  'commands/core/variables/tests/Variable replacement tests.spec.yaml',
  'commands/core/script-info/tests/Input parameters tests.spec.yaml',
  'commands/core/script-info/tests/Input schema tests.spec.yaml',
  'commands/core/control-flow/tests/Do tests.spec.yaml',
  'commands/core/control-flow/tests/Exit tests.spec.yaml',
  'commands/core/control-flow/tests/empty.spec.yaml',
]

/** Tests that depend on commands from higher levels (skip at Level 0) */
const SKIP_TESTS = new Set([
  'SCRIPT_HOME is different from SCRIPT_TEMP_DIR', // needs Temp file (Level 3)
])

/**
 * Run a .spec.yaml file as a Vitest test suite.
 *
 * SpecScript test files use one of two structures:
 *
 * 1. Flat: Test case commands mixed with other commands
 *    ```
 *    Test case: name
 *    Output: something
 *    Expected output: something
 *    ```
 *
 * 2. Structured: Tests/Before all tests/After all tests sections
 *    ```
 *    Tests:
 *      test name:
 *        Output: something
 *        Expected output: something
 *    ```
 *
 * In structure (2), each key under Tests: is a test case name,
 * and its value is the test body (a list of commands).
 */
function runSpecFile(relativePath: string): void {
  const fullPath = join(SPEC_DIR, relativePath)
  const content = readFileSync(fullPath, 'utf-8')

  // Parse the file using command-aware parser (preserves duplicate keys)
  const commands = parseYamlCommands(content)
  const script = new Script(commands)

  // Check for structured test format (Tests/Before all tests/After all tests)
  const hasTestsSections = commands.some(c => {
    const name = c.name.toLowerCase()
    return name === 'tests' || name === 'before all tests' || name === 'after all tests'
  })

  if (hasTestsSections) {
    runStructuredTests(script, fullPath)
  } else {
    runFlatTests(script, relativePath, fullPath)
  }
}

/**
 * Handle structured test files with Tests:/Before all tests:/After all tests: sections.
 */
function runStructuredTests(script: Script, fullPath: string): void {
  // Find the sections
  let setupCommands: JsonValue | undefined
  let testsData: JsonValue | undefined
  let teardownCommands: JsonValue | undefined

  for (const cmd of script.commands) {
    const name = cmd.name.toLowerCase()
    if (name === 'before all tests') setupCommands = cmd.data
    else if (name === 'tests') testsData = cmd.data
    else if (name === 'after all tests') teardownCommands = cmd.data
  }

  let sharedContext: DefaultContext | undefined

  if (setupCommands !== undefined) {
    beforeAll(() => {
      sharedContext = new DefaultContext({ scriptFile: fullPath })
      setupSilentCapture(sharedContext)
      const setupScript = Script.fromData(setupCommands!)
      setupScript.run(sharedContext)
    })
  }

  if (teardownCommands !== undefined) {
    afterAll(() => {
      if (sharedContext) {
        const teardownScript = Script.fromData(teardownCommands!)
        teardownScript.run(sharedContext)
      }
    })
  }

  // Each key in the Tests: object is a test case
  if (testsData && isObject(testsData)) {
    for (const [testName, testBody] of Object.entries(testsData)) {
      if (SKIP_TESTS.has(testName)) {
        it.skip(testName, () => {})
        continue
      }
      it(testName, () => {
        const context = sharedContext ? sharedContext.clone() as DefaultContext : new DefaultContext({ scriptFile: fullPath })
        setupSilentCapture(context)
        const testScript = Script.fromData(testBody)
        testScript.run(context)
      })
    }
  }
}

/**
 * Handle flat test files that use Test case: commands as boundaries.
 */
function runFlatTests(script: Script, relativePath: string, fullPath: string): void {
  const testCases = script.splitTestCases()

  if (testCases.length === 1 && testCases[0].name === 'default') {
    it(relativePath, () => {
      const context = new DefaultContext({ scriptFile: fullPath })
      setupSilentCapture(context)
      script.run(context)
    })
  } else {
    for (const testCase of testCases) {
      it(testCase.name, () => {
        const context = new DefaultContext({ scriptFile: fullPath })
        setupSilentCapture(context)
        testCase.script.run(context)
      })
    }
  }
}

// --- Generate test suites ---

describe('Level 0 Spec Tests', () => {
  for (const file of LEVEL_0_TEST_FILES) {
    describe(file, () => {
      runSpecFile(file)
    })
  }
})
