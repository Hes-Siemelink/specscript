import { readFileSync, existsSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { describe, it, beforeAll, afterAll, expect } from 'vitest'
import { Script, toCommandList } from '../src/language/script.js'
import { DefaultContext } from '../src/language/context.js'
import { registerAllCommands } from '../src/commands/register.js'
import { setupSilentCapture } from '../src/language/stdout-capture.js'
import type { JsonValue } from '../src/language/types.js'
import { isObject } from '../src/language/types.js'
import { parseYamlCommands } from '../src/util/yaml.js'

// Register commands once
registerAllCommands()

/** Per-test timeout in ms — guards against infinite loops from Repeat/ForEach */
const TEST_TIMEOUT = 10_000

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

/** Level 1 spec.yaml test files (relative to specification/) */
const LEVEL_1_TEST_FILES = [
  'language/tests/Eval tests.spec.yaml',
  'commands/core/control-flow/tests/If tests.spec.yaml',
  'commands/core/control-flow/tests/For each tests.spec.yaml',
  'commands/core/control-flow/tests/Repeat tests.spec.yaml',
  'commands/core/errors/tests/Error handling tests.spec.yaml',
  'commands/core/data-manipulation/tests/Add tests.spec.yaml',
  'commands/core/data-manipulation/tests/Append tests.spec.yaml',
  'commands/core/data-manipulation/tests/Json patch tests.spec.yaml',
  'commands/core/data-manipulation/tests/Replace tests.spec.yaml',
  'commands/core/data-manipulation/tests/Size tests.spec.yaml',
  'commands/core/data-manipulation/tests/Sort tests.spec.yaml',
  'commands/core/util/tests/Base64 tests.spec.yaml',
  'commands/core/util/tests/Wait tests.spec.yaml',
]

/** Tests that depend on commands from higher levels (skip) */
const SKIP_TESTS = new Set([
  'SCRIPT_HOME is different from SCRIPT_TEMP_DIR',        // needs Temp file (Level 3)
  'Schema validation - Add should only accept arrays',     // needs Validate schema (Level 5)
  'For each with variable syntax in sample data',          // needs Read file (Level 3)
  'For each with variable syntax in sample data and implicit loop variable', // needs Read file (Level 3)
])

/** Test files to skip entirely (all tests use higher-level commands) */
const SKIP_FILES = new Set([
  'commands/core/control-flow/schema/Schema tests.spec.yaml', // needs Validate schema (Level 5)
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
      }, TEST_TIMEOUT)
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
    }, TEST_TIMEOUT)
  } else {
    for (const testCase of testCases) {
      it(testCase.name, () => {
        const context = new DefaultContext({ scriptFile: fullPath })
        setupSilentCapture(context)
        testCase.script.run(context)
      }, TEST_TIMEOUT)
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

describe('Level 1 Spec Tests', () => {
  for (const file of LEVEL_1_TEST_FILES) {
    if (SKIP_FILES.has(file)) {
      describe.skip(file, () => {
        it.skip('all tests skipped (higher-level dependency)', () => {})
      })
      continue
    }
    describe(file, () => {
      runSpecFile(file)
    })
  }
})
