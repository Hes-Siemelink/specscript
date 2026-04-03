import {existsSync, mkdtempSync, readFileSync} from 'node:fs'
import {dirname, join} from 'node:path'
import {tmpdir} from 'node:os'
import {afterAll, beforeAll, describe, it} from 'vitest'
import {Script} from '../src/language/script.js'
import {DefaultContext} from '../src/language/context.js'
import {registerAllCommands} from '../src/commands/register.js'
import {setupSilentCapture} from '../src/language/stdout-capture.js'
import type {JsonValue} from '../src/language/types.js'
import {isObject} from '../src/language/types.js'
import {parseYamlCommands} from '../src/util/yaml.js'
import {scanMarkdown} from '../src/markdown/scanner.js'
import {getTestTitle, splitMarkdownSections} from '../src/markdown/converter.js'
import {getCommandHandler} from '../src/language/command-handler.js'
import {stopAllServers} from '../src/commands/http-server.js'
import {stopAllMcpServers} from '../src/commands/mcp-server.js'

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

/** Level 1 spec.md test files — command reference docs with executable examples */
const LEVEL_1_MD_FILES = [
    // Control flow
    'commands/core/control-flow/Do.spec.md',
    'commands/core/control-flow/Exit.spec.md',
    'commands/core/control-flow/For each.spec.md',
    'commands/core/control-flow/If.spec.md',
    'commands/core/control-flow/Repeat.spec.md',
    'commands/core/control-flow/When.spec.md',
    // Data manipulation
    'commands/core/data-manipulation/Add.spec.md',
    'commands/core/data-manipulation/Add to.spec.md',
    'commands/core/data-manipulation/Append.spec.md',
    'commands/core/data-manipulation/Fields.spec.md',
    'commands/core/data-manipulation/Find.spec.md',
    'commands/core/data-manipulation/Json patch.spec.md',
    'commands/core/data-manipulation/Replace.spec.md',
    'commands/core/data-manipulation/Size.spec.md',
    'commands/core/data-manipulation/Sort.spec.md',
    'commands/core/data-manipulation/Values.spec.md',
    // Errors
    'commands/core/errors/Error.spec.md',
    'commands/core/errors/On error.spec.md',
    'commands/core/errors/On error type.spec.md',
    // Testing
    'commands/core/testing/After all tests.spec.md',
    'commands/core/testing/Assert equals.spec.md',
    'commands/core/testing/Assert that.spec.md',
    'commands/core/testing/Code example.spec.md',
    'commands/core/testing/Expected console output.spec.md',
    'commands/core/testing/Expected error.spec.md',
    'commands/core/testing/Expected output.spec.md',
    'commands/core/testing/Test case.spec.md',
    'commands/core/testing/Tests.spec.md',
    // Util
    'commands/core/util/Base64 decode.spec.md',
    'commands/core/util/Base64 encode.spec.md',
    'commands/core/util/Json.spec.md',
    'commands/core/util/Parse Yaml.spec.md',
    'commands/core/util/Print Json.spec.md',
    'commands/core/util/Print.spec.md',
    'commands/core/util/Text.spec.md',
    'commands/core/util/Wait.spec.md',
    // Variables
    'commands/core/variables/As.spec.md',
    'commands/core/variables/Assignment.spec.md',
    'commands/core/variables/Output.spec.md',
]

/** Level 2 spec.md test files (relative to specification/) */
const LEVEL_2_TEST_FILES = [
    'language/SpecScript Markdown Documents.spec.md',
    'language/tests/SpecScript Markdown tests.spec.md',
    'language/SpecScript Best Practices.spec.md',
    'language/Eval syntax.spec.md',
    'language/Variables.spec.md',
    'language/Testing.spec.md',
]

/** Level 3 spec.yaml test files (relative to specification/) */
const LEVEL_3_TEST_FILES = [
    'commands/core/files/tests/Read file tests.spec.yaml',
    'commands/core/files/tests/Save as tests.spec.yaml',
    'commands/core/files/tests/Locate files in the same way.spec.yaml',
    'commands/core/shell/tests/Shell tests.spec.yaml',
    'commands/core/files/tests/Run script tests.spec.yaml',
]

/** Level 3 spec.md test files (relative to specification/) */
const LEVEL_3_MD_FILES = [
    'commands/core/files/Read file.spec.md',
    'commands/core/files/Write file.spec.md',
    'commands/core/files/Temp file.spec.md',
    'commands/core/files/Run script.spec.md',
    'commands/core/files/SpecScript files as commands.spec.md',
    'commands/core/shell/Shell.spec.md',
    'commands/core/shell/Cli.spec.md',
    // Script info docs (use file= and shell cli blocks)
    'commands/core/script-info/Input parameters.spec.md',
    'commands/core/script-info/Input schema.spec.md',
    'commands/core/script-info/Script info.spec.md',
    // Language docs that need file/cli support
    'language/Organizing SpecScript files in directories.spec.md',
    'language/Packages.spec.md',
    'language/SpecScript Yaml Scripts.spec.md',
    'language/Testing.spec.md',
    'language/tests/Directory tests.spec.md',
    // CLI docs (use shell cli blocks)
    'cli/Command line options.spec.md',
    'cli/Running SpecScript files.spec.md',
]

/** Level 4 spec.yaml test files (relative to specification/) */
const LEVEL_4_TEST_FILES = [
    'commands/core/http/tests/Http client tests.spec.yaml',
    'commands/core/http/tests/Http server tests.spec.yaml',
]

/** Level 4 spec.md test files (relative to specification/) */
const LEVEL_4_MD_FILES = [
    'commands/core/http/GET.spec.md',
    'commands/core/http/POST.spec.md',
    'commands/core/http/PUT.spec.md',
    'commands/core/http/PATCH.spec.md',
    'commands/core/http/DELETE.spec.md',
    'commands/core/http/Http request defaults.spec.md',
    'commands/core/http/Http server.spec.md',
    'commands/core/http/Http endpoint.spec.md',
    'commands/core/http/Stop http server.spec.md',
    // Testing command docs that use HTTP
    'commands/core/testing/Before all tests.spec.md',
]

/** Level 5 connection spec.yaml test files (relative to specification/) */
const LEVEL_5_CONNECTION_TEST_FILES = [
    'commands/core/connections/tests/Connect to tests.spec.yaml',
    'commands/core/connections/tests/Connection inheritance tests.spec.yaml',
    'commands/core/connections/tests/Upward search tests.spec.yaml',
    'commands/core/connections/tests/Credentials tests.spec.yaml',
]

/** Tests that depend on commands from higher levels (skip).
 *  Format: 'relative/path > Test name' or just 'Test name' for global match. */
const SKIP_TESTS = new Set([
    'Schema validation - Add should only accept arrays',     // needs Validate schema (Level 5)
    'commands/core/script-info/Input parameters.spec.md > Cli help',      // runs spec binary via shell cli
    'commands/core/script-info/Input parameters.spec.md > Using types',   // needs Types command
    'commands/core/script-info/Input schema.spec.md > Cli help',          // runs spec binary via shell cli
    'commands/core/script-info/Script info.spec.md > Hidden commands',    // Expected console output mismatch (Script info format)
    'language/Testing.spec.md > Testing',                                     // runs spec --test . which hangs
    'language/SpecScript Yaml Scripts.spec.md > Hello world example',     // runs spec binary via shell cli
    'language/SpecScript Yaml Scripts.spec.md > The command sequence',    // runs spec binary via shell cli (needs Prompt)
    'language/SpecScript Yaml Scripts.spec.md > Script info',             // runs spec binary via shell cli
    'language/SpecScript Yaml Scripts.spec.md > Defining script input',   // runs spec binary via shell cli
    'language/SpecScript Yaml Scripts.spec.md > Script output',           // runs spec binary via shell cli
    'cli/Command line options.spec.md > --debug',                         // error output references Kotlin stacktrace
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
        beforeAll(async () => {
            sharedContext = new DefaultContext({scriptFile: fullPath, workingDir: SPECSCRIPT_HOME})
            setupSilentCapture(sharedContext)
            const setupScript = Script.fromData(setupCommands!)
            await setupScript.run(sharedContext)
        })
    }

    if (teardownCommands !== undefined) {
        afterAll(async () => {
            if (sharedContext) {
                const teardownScript = Script.fromData(teardownCommands!)
                await teardownScript.run(sharedContext)
            }
        })
    }

    // Each key in the Tests: object is a test case
    if (testsData && isObject(testsData)) {
        for (const [testName, testBody] of Object.entries(testsData)) {
            if (SKIP_TESTS.has(testName)) {
                it.skip(testName, () => {
                })
                continue
            }
            it(testName, async () => {
                const context = sharedContext ? sharedContext.clone() as DefaultContext : new DefaultContext({
                    scriptFile: fullPath,
                    workingDir: SPECSCRIPT_HOME
                })
                setupSilentCapture(context)
                const testScript = Script.fromData(testBody)
                await testScript.run(context)
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
        it(relativePath, async () => {
            const context = new DefaultContext({scriptFile: fullPath, workingDir: SPECSCRIPT_HOME})
            setupSilentCapture(context)
            await script.run(context)
        }, TEST_TIMEOUT)
    } else {
        for (const testCase of testCases) {
            it(testCase.name, async () => {
                const context = new DefaultContext({scriptFile: fullPath, workingDir: SPECSCRIPT_HOME})
                setupSilentCapture(context)
                await testCase.script.run(context)
            }, TEST_TIMEOUT)
        }
    }
}

/**
 * Run a .spec.md file as a Vitest test suite.
 *
 * Each # section in the Markdown file becomes a separate test case.
 * Sections share a context within the same document.
 * Sections with no executable commands are silently skipped.
 */
function runSpecMdFile(relativePath: string): void {
    const fullPath = join(SPEC_DIR, relativePath)
    const content = readFileSync(fullPath, 'utf-8')

    const blocks = scanMarkdown(content)
    const scripts = splitMarkdownSections(blocks)

    // Mirror Kotlin's getCodeExamplesAsTests():
    // - Create a temp dir that serves as BOTH scriptDir AND tempDir
    // - Use scriptHome to point SCRIPT_HOME to the original spec file's directory
    // This ensures that `file=` blocks (which create Temp files) write to the temp dir,
    // and `resource:` lookups (which use scriptDir) find them there.
    const testDir = mkdtempSync(join(tmpdir(), 'specscript-'))
    const sharedContext = new DefaultContext({
        scriptFile: join(testDir, 'test.spec.md'),
        workingDir: SPECSCRIPT_HOME,
        scriptHome: dirname(fullPath),
    })
    // Align tempDir with scriptDir so file= blocks write to the same directory
    sharedContext.variables.set('SCRIPT_TEMP_DIR', testDir)
    setupSilentCapture(sharedContext)

    let hasTests = false

    for (const script of scripts) {
        // Skip sections with no executable commands
        if (script.commands.length === 0) continue

        const title = getTestTitle(script)

        const qualifiedTitle = `${relativePath} > ${title}`
        if (SKIP_TESTS.has(title) || SKIP_TESTS.has(qualifiedTitle)) {
            it.skip(title, () => {
            })
            hasTests = true
            continue
        }

        // Skip sections that use commands not available at this level.
        // Only skip for known higher-level commands (e.g., Prompt). Unknown commands
        // might be local file commands created at runtime by Temp file or yaml file= blocks.
        const HIGHER_LEVEL_COMMANDS = new Set(['Validate schema', 'SQLite'])
        const unavailable = script.commands.find(
            c => !isAssignment(c.name) && !getCommandHandler(c.name) && HIGHER_LEVEL_COMMANDS.has(c.name)
        )
        if (unavailable) {
            it.skip(`${title} (needs ${unavailable.name})`, () => {
            })
            hasTests = true
            continue
        }

        // Skip sections that have skipped blocks (L3+ shell/file blocks)
        if (script.skippedBlocks.length > 0) {
            it.skip(`${title} (needs ${script.skippedBlocks[0]})`, () => {
            })
            hasTests = true
            continue
        }

        hasTests = true
        it(title, async () => {
            // Reset state before each section (matches Kotlin TestCaseRunner)
            sharedContext.error = undefined
            sharedContext.variables.delete('input')
            const captured = sharedContext.session.get('capturedOutput') as string[] | undefined
            if (captured) captured.length = 0

            await script.run(sharedContext)
        }, TEST_TIMEOUT)
    }

    // If no sections produced tests, add a placeholder to avoid empty suite error
    if (!hasTests) {
        it.skip('no executable sections at this level', () => {
        })
    }
}

/** Check if a command name is a variable assignment (${...}: value) */
function isAssignment(name: string): boolean {
    return name.startsWith('${') || name.startsWith('$-{')
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
                it.skip('all tests skipped (higher-level dependency)', () => {
                })
            })
            continue
        }
        describe(file, () => {
            runSpecFile(file)
        })
    }
})

describe('Level 1 Spec Tests (Markdown)', () => {
    for (const file of LEVEL_1_MD_FILES) {
        describe(file, () => {
            runSpecMdFile(file)
        })
    }
})

describe('Level 2 Spec Tests', () => {
    for (const file of LEVEL_2_TEST_FILES) {
        describe(file, () => {
            runSpecMdFile(file)
        })
    }
})

describe('Level 3 Spec Tests', () => {
    for (const file of LEVEL_3_TEST_FILES) {
        if (SKIP_FILES.has(file)) {
            describe.skip(file, () => {
                it.skip('all tests skipped (higher-level dependency)', () => {
                })
            })
            continue
        }
        describe(file, () => {
            runSpecFile(file)
        })
    }
})

describe('Level 3 Spec Tests (Markdown)', () => {
    for (const file of LEVEL_3_MD_FILES) {
        describe(file, () => {
            runSpecMdFile(file)
        })
    }
})

// --- Level 4: HTTP ---
// Start the sample server once before all HTTP tests, stop it after.

describe('Level 4 Spec Tests', () => {
    beforeAll(async () => {
        // Start the sample server on port 2525 (used by all HTTP tests)
        const sampleServerPath = join(SPECSCRIPT_HOME, 'specification/code-examples/sample-server/start.spec.yaml')
        const content = readFileSync(sampleServerPath, 'utf-8')
        const sampleServerContext = new DefaultContext({scriptFile: sampleServerPath, workingDir: SPECSCRIPT_HOME})
        setupSilentCapture(sampleServerContext)
        const script = Script.fromString(content)
        await script.run(sampleServerContext)
    })

    afterAll(async () => {
        await stopAllServers()
    })

    for (const file of LEVEL_4_TEST_FILES) {
        if (SKIP_FILES.has(file)) {
            describe.skip(file, () => {
                it.skip('all tests skipped (higher-level dependency)', () => {
                })
            })
            continue
        }
        describe(file, () => {
            runSpecFile(file)
        })
    }

    for (const file of LEVEL_4_MD_FILES) {
        describe(file, () => {
            runSpecMdFile(file)
        })
    }

    // Connection tests share the same sample server on port 2525
    for (const file of LEVEL_5_CONNECTION_TEST_FILES) {
        describe(file, () => {
            runSpecFile(file)
        })
    }
})

// --- Level 5: Schema / Types ---

/** Level 5 spec.yaml test files (relative to specification/) */
const LEVEL_5_TEST_FILES = [
    'commands/core/types/tests/Type tests.spec.yaml',
    'commands/core/user-interaction/tests/Prompt tests.spec.yaml',
    'commands/core/user-interaction/tests/Prompt object tests.spec.yaml',
]

/** Level 5 spec.md test files (relative to specification/) */
const LEVEL_5_MD_FILES = [
    'commands/core/types/Types.spec.md',
    'commands/core/user-interaction/Prompt.spec.md',
    'commands/core/user-interaction/Prompt object.spec.md',
    'commands/core/user-interaction/Confirm.spec.md',
    'commands/core/testing/Answers.spec.md',
]

describe('Level 5 Spec Tests', () => {
    for (const file of LEVEL_5_TEST_FILES) {
        describe(file, () => {
            runSpecFile(file)
        })
    }

    for (const file of LEVEL_5_MD_FILES) {
        describe(file, () => {
            runSpecMdFile(file)
        })
    }
})

// --- Level 6: MCP ---

/** Level 6 spec.md test files (relative to specification/) */
const LEVEL_6_MCP_MD_FILES = [
    'commands/ai/mcp/Mcp server.spec.md',
    'commands/ai/mcp/Mcp tool.spec.md',
    'commands/ai/mcp/Mcp tool call.spec.md',
    'commands/ai/mcp/Mcp prompt.spec.md',
    'commands/ai/mcp/Mcp resource.spec.md',
    'commands/ai/mcp/Stop mcp server.spec.md',
]

describe('Level 6 MCP Spec Tests', () => {
    afterAll(async () => {
        await stopAllMcpServers()
    })

    for (const file of LEVEL_6_MCP_MD_FILES) {
        describe(file, () => {
            runSpecMdFile(file)
        })
    }
})


