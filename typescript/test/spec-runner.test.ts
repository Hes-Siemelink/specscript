import {existsSync, readdirSync, readFileSync, mkdtempSync} from 'node:fs'
import {dirname, join, relative, sep} from 'node:path'
import {tmpdir} from 'node:os'
import {afterAll, beforeAll, describe, it} from 'vitest'
import {Script} from '../src/language/script.js'
import type {Command} from '../src/language/types.js'
import {DefaultContext} from '../src/language/context.js'
import {registerAllCommands} from '../src/commands/register.js'
import {setupSilentCapture} from '../src/language/stdout-capture.js'
import type {JsonValue} from '../src/language/types.js'
import {isObject} from '../src/language/types.js'
import {parseYamlCommands} from '../src/util/yaml.js'
import {scanMarkdown} from '../src/markdown/scanner.js'
import {getTestTitle, splitMarkdownSections} from '../src/markdown/converter.js'
import {stopAllServers} from '../src/commands/http-server.js'
import {stopAllMcpServers} from '../src/commands/mcp-server.js'

// Register commands once
registerAllCommands()

/** Per-test timeout in ms — guards against infinite loops from Repeat/ForEach */
const TEST_TIMEOUT = 10_000

/** Find the SpecScript home directory (repo root with specification/). */
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
const SAMPLE_SERVER = 'code-examples/sample-server/start.spec.yaml'

/**
 * Known test-level exceptions. Format: 'relative/path > Test name' or 'Test name' for global match.
 *
 * Discovery runs every spec file it finds. Tests here are either:
 *  (a) intentionally Kotlin-specific (stacktraces, spec binary invocations),
 *  (b) rely on commands not yet implemented in TypeScript (Validate schema),
 *  (c) rely on Kotlin behavior not yet ported (local file command discovery in Script info).
 */
const SKIP_TESTS = new Set([
    // (a) Kotlin-specific output / behavior
    'commands/core/script-info/Input parameters.spec.md > Using types',

    // (b) Validate schema not yet implemented in TypeScript
    'commands/core/schema/Validate schema.spec.md > Basic usage',
    'commands/core/schema/Validate schema.spec.md > Schema from file',
    'commands/core/schema/Validate schema.spec.md > Invalid data',
    'No extra properties in If',
    'If without then',
    "When with both 'then' and 'else'",
    'Validate conditions in When',
    'Validation with external schema',
    'Validation with external schema -- failure',
])

/** Whole files to skip (all tests inside would fail on a missing command). */
const SKIP_FILES = new Set([
    'commands/core/schema/tests/Validate tests.spec.yaml',   // Validate schema (Level 5) not implemented
    'commands/core/control-flow/schema/Schema tests.spec.yaml', // uses Validate schema
])

// --- Discovery ---------------------------------------------------------------

/** Files/dirs to exclude from discovery entirely (paths relative to SPEC_DIR). */
const EXCLUDED_PATHS = new Set([SAMPLE_SERVER])

function discoverSpecFiles(): string[] {
    const entries = readdirSync(SPEC_DIR, {recursive: true, withFileTypes: true})
    const files: string[] = []
    for (const e of entries) {
        if (!e.isFile()) continue
        const abs = join(e.parentPath ?? (e as unknown as {path: string}).path, e.name)
        const rel = relative(SPEC_DIR, abs).split(sep).join('/')
        if (!rel.endsWith('.spec.yaml') && !rel.endsWith('.spec.md')) continue
        if (EXCLUDED_PATHS.has(rel)) continue
        files.push(rel)
    }
    return files.sort()
}

/** Returns true if a .spec.yaml file contains Tests: or Test case: (matches Kotlin filter). */
function yamlHasTests(commands: Command[]): boolean {
    return commands.some(c => {
        const n = c.name.toLowerCase()
        return n === 'tests' || n === 'test case'
    })
}

// --- Runners -----------------------------------------------------------------

function runSpecYaml(relativePath: string, commands: Command[]): void {
    const fullPath = join(SPEC_DIR, relativePath)
    const script = new Script(commands)

    const hasTestsSections = commands.some(c => {
        const n = c.name.toLowerCase()
        return n === 'tests' || n === 'before all tests' || n === 'after all tests'
    })

    if (hasTestsSections) {
        runStructuredTests(script, fullPath)
    } else {
        runFlatTests(script, relativePath, fullPath)
    }
}

function runStructuredTests(script: Script, fullPath: string): void {
    let setupCommands: JsonValue | undefined
    let testsData: JsonValue | undefined
    let teardownCommands: JsonValue | undefined

    for (const cmd of script.commands) {
        const n = cmd.name.toLowerCase()
        if (n === 'before all tests') setupCommands = cmd.data
        else if (n === 'tests') testsData = cmd.data
        else if (n === 'after all tests') teardownCommands = cmd.data
    }

    let sharedContext: DefaultContext | undefined

    if (setupCommands !== undefined) {
        beforeAll(async () => {
            sharedContext = new DefaultContext({scriptFile: fullPath, workingDir: dirname(fullPath)})
            setupSilentCapture(sharedContext)
            await Script.fromData(setupCommands!).run(sharedContext)
        })
    }

    if (teardownCommands !== undefined) {
        afterAll(async () => {
            if (sharedContext) {
                await Script.fromData(teardownCommands!).run(sharedContext)
            }
        })
    }

    if (testsData && isObject(testsData)) {
        for (const [testName, testBody] of Object.entries(testsData)) {
            if (SKIP_TESTS.has(testName)) {
                it.skip(testName, () => {})
                continue
            }
            it(testName, async () => {
                const context = sharedContext
                    ? (sharedContext.clone() as DefaultContext)
                    : new DefaultContext({scriptFile: fullPath, workingDir: dirname(fullPath)})
                setupSilentCapture(context)
                await Script.fromData(testBody as JsonValue).run(context)
            }, TEST_TIMEOUT)
        }
    }
}

function runFlatTests(script: Script, relativePath: string, fullPath: string): void {
    const testCases = script.splitTestCases()
    if (testCases.length === 1 && testCases[0].name === 'default') {
        it(relativePath, async () => {
            const context = new DefaultContext({scriptFile: fullPath, workingDir: dirname(fullPath)})
            setupSilentCapture(context)
            await script.run(context)
        }, TEST_TIMEOUT)
    } else {
        for (const testCase of testCases) {
            it(testCase.name, async () => {
                const context = new DefaultContext({scriptFile: fullPath, workingDir: dirname(fullPath)})
                setupSilentCapture(context)
                await testCase.script.run(context)
            }, TEST_TIMEOUT)
        }
    }
}

function runMarkdownFile(fullPath: string, displayPath: string, scriptHome: string): void {
    const content = readFileSync(fullPath, 'utf-8')
    const blocks = scanMarkdown(content)
    const scripts = splitMarkdownSections(blocks)

    const testDir = mkdtempSync(join(tmpdir(), 'specscript-'))
    const sharedContext = new DefaultContext({
        scriptFile: join(testDir, 'test.spec.md'),
        workingDir: SPECSCRIPT_HOME,
        scriptHome,
    })
    sharedContext.variables.set('SCRIPT_TEMP_DIR', testDir)
    setupSilentCapture(sharedContext)

    let hasTests = false

    for (const script of scripts) {
        if (script.commands.length === 0) continue

        const title = getTestTitle(script)
        const qualifiedTitle = `${displayPath} > ${title}`

        if (SKIP_TESTS.has(title) || SKIP_TESTS.has(qualifiedTitle)) {
            it.skip(title, () => {})
            hasTests = true
            continue
        }

        // Skip sections with blocks the scanner flagged as unsupported (e.g. shell cli, yaml file=)
        if (script.skippedBlocks.length > 0) {
            it.skip(`${title} (needs ${script.skippedBlocks[0]})`, () => {})
            hasTests = true
            continue
        }

        hasTests = true
        it(title, async () => {
            sharedContext.error = undefined
            sharedContext.variables.delete('input')
            const captured = sharedContext.session.get('capturedOutput') as string[] | undefined
            if (captured) captured.length = 0
            await script.run(sharedContext)
        }, TEST_TIMEOUT)
    }

    if (!hasTests) {
        it.skip('no executable sections', () => {})
    }
}

// --- Suite -------------------------------------------------------------------

describe('SpecScript specification', () => {
    beforeAll(async () => {
        // Start sample server (used by HTTP and connection tests)
        const sampleServerPath = join(SPEC_DIR, SAMPLE_SERVER)
        const content = readFileSync(sampleServerPath, 'utf-8')
        const context = new DefaultContext({scriptFile: sampleServerPath, workingDir: SPECSCRIPT_HOME})
        setupSilentCapture(context)
        await Script.fromString(content).run(context)
    })

    afterAll(async () => {
        await stopAllServers()
        await stopAllMcpServers()
    })

    // Project READMEs (Kotlin runs these via SpecScriptTestSuite.Main README_md)
    for (const readme of ['README.md', 'README-old.md']) {
        const abs = join(SPECSCRIPT_HOME, readme)
        if (!existsSync(abs)) continue
        describe(readme, () => runMarkdownFile(abs, readme, SPECSCRIPT_HOME))
    }

    for (const relativePath of discoverSpecFiles()) {
        if (relativePath.endsWith('.spec.yaml')) {
            registerYamlFile(relativePath)
        } else {
            const abs = join(SPEC_DIR, relativePath)
            describe(relativePath, () => runMarkdownFile(abs, relativePath, dirname(abs)))
        }
    }
})

function registerYamlFile(relativePath: string): void {
    if (SKIP_FILES.has(relativePath)) {
        describe.skip(relativePath, () => it.skip('', () => {}))
        return
    }

    const fullPath = join(SPEC_DIR, relativePath)
    let commands: Command[]
    try {
        commands = parseYamlCommands(readFileSync(fullPath, 'utf-8'))
    } catch (e) {
        describe(relativePath, () => {
            it('parse', () => { throw e })
        })
        return
    }

    // Kotlin filter: .spec.yaml files without Tests: or Test case: are helpers, not tests
    if (!yamlHasTests(commands)) return

    describe(relativePath, () => runSpecYaml(relativePath, commands))
}
