import { readFileSync, existsSync, readdirSync, statSync } from 'node:fs'
import { resolve, dirname, basename } from 'node:path'
import { Script } from './language/script.js'
import { DefaultContext } from './language/context.js'
import type { ScriptContext } from './language/context.js'
import { registerAllCommands } from './commands/register.js'
import { setupStdoutCapture } from './language/stdout-capture.js'
import { scanMarkdown } from './markdown/scanner.js'
import { splitMarkdownSections } from './markdown/converter.js'
import { toDisplayYaml } from './util/yaml.js'
import { parseYamlCommands } from './util/yaml.js'
import type { JsonValue, JsonObject, Command } from './language/types.js'
import { SpecScriptError, SpecScriptCommandError, MissingInputError, isObject } from './language/types.js'
import { setPackagePath } from './language/package-registry.js'

// ---------------------------------------------------------------------------
// Global option definitions (mirrors specscript-command-line-options.yaml)
// ---------------------------------------------------------------------------

interface OptionDef {
  description: string
  shortOption: string
  valueBearing?: boolean
}

const GLOBAL_OPTIONS: Record<string, OptionDef> = {
  'help':         { description: 'Print help on a script or directory and does not run anything', shortOption: 'h' },
  'no-output':    { description: 'Suppress the output at the end of the script', shortOption: 'n' },
  'output-json':  { description: 'Print the output at the end of the script in Json format', shortOption: 'j' },
  'interactive':  { description: 'SpecScript may prompt for user input if it needs more information', shortOption: 'i' },
  'debug':        { description: 'Run in debug mode. Prints stacktraces when an error occurs.', shortOption: 'd' },
  'test':         { description: 'Run in test mode. Only tests will be executed.', shortOption: 't' },
  'package-path': { description: 'Directory containing packages', shortOption: 'p', valueBearing: true },
}

// ---------------------------------------------------------------------------
// Parsed options
// ---------------------------------------------------------------------------

type OutputOption = 'yaml' | 'json' | 'none'

interface CliOptions {
  help: boolean
  printOutput: OutputOption
  debug: boolean
  testMode: boolean
  interactive: boolean
  packagePath: string | undefined
  commands: string[]
  commandArgs: string[]
}

// ---------------------------------------------------------------------------
// Formatting (mirrors Kotlin's infoString + toDisplayString)
// ---------------------------------------------------------------------------

function infoString(key: string, value: string, width: number): string {
  return `${key.padEnd(width)}   ${value.trim()}`
}

function formatOptions(options: Record<string, OptionDef>): string {
  const entries = Object.entries(options)
  const keySummaries = entries.map(([name, def]) => {
    let key = `--${name}`
    if (def.shortOption) key += `, -${def.shortOption}`
    return key
  })
  const width = Math.max(...keySummaries.map(k => k.length))
  return entries.map(([, def], i) =>
    `  ${infoString(keySummaries[i], def.description, width)}`
  ).join('\n') + '\n'
}

/**
 * Format script info properties for --help display.
 * Mirrors Kotlin's ObjectDefinition.toDisplayString().
 */
function formatProperties(properties: Record<string, { description?: string; shortOption?: string }>): string {
  const entries = Object.entries(properties)
  const keySummaries = entries.map(([name, def]) => {
    let key = `--${name}`
    if (def.shortOption) key += `, -${def.shortOption}`
    return key
  })
  const width = Math.max(...keySummaries.map(k => k.length))
  return entries.map(([, def], i) =>
    `  ${infoString(keySummaries[i], def.description ?? '', width)}`
  ).join('\n') + '\n'
}

// ---------------------------------------------------------------------------
// Argument parsing (mirrors Kotlin's splitArguments + CliCommandLineOptions)
// ---------------------------------------------------------------------------

class CliInvocationError extends Error {
  constructor(message: string) { super(message) }
}

function parseCliOptions(args: string[]): CliOptions {
  const { globalArgs, commands, commandArgs } = splitArguments(args)

  // Validate global args against known options
  const activeOptions = new Set<string>()
  let packagePath: string | undefined
  for (const [key, value] of globalArgs) {
    const option = resolveOption(key)
    activeOptions.add(option)
    if (option === 'package-path' && value !== undefined) {
      packagePath = value
    }
  }

  const printOutput: OutputOption = activeOptions.has('no-output')
    ? 'none'
    : activeOptions.has('output-json')
      ? 'json'
      : 'yaml'

  return {
    help: activeOptions.has('help'),
    printOutput,
    debug: activeOptions.has('debug'),
    testMode: activeOptions.has('test'),
    interactive: activeOptions.has('interactive'),
    packagePath,
    commands,
    commandArgs,
  }
}

function resolveOption(normalized: string): string {
  for (const [name, def] of Object.entries(GLOBAL_OPTIONS)) {
    if (name === normalized || def.shortOption === normalized) return name
  }
  throw new CliInvocationError(`Invalid option: ${normalized}`)
}

function splitArguments(args: string[]): {
  globalArgs: Array<[string, string | undefined]>
  commands: string[]
  commandArgs: string[]
} {
  const globalArgs: Array<[string, string | undefined]> = []
  const commands: string[] = []
  const commandArgs: string[] = []

  let state: 'global' | 'commands' | 'commandArgs' = 'global'
  let expectValue: string | undefined

  for (const arg of args) {
    if (expectValue !== undefined) {
      globalArgs.push([expectValue, arg])
      expectValue = undefined
      continue
    }

    if (state === 'global') {
      if (isFlag(arg)) {
        const normalized = normalize(arg)
        if (isValueBearingOption(normalized)) {
          expectValue = normalized
        } else {
          globalArgs.push([normalized, undefined])
        }
      } else {
        state = 'commands'
      }
    }

    if (state === 'commands') {
      if (isFlag(arg)) {
        state = 'commandArgs'
      } else {
        commands.push(arg)
      }
    }

    if (state === 'commandArgs') {
      commandArgs.push(arg)
    }
  }

  return { globalArgs, commands, commandArgs }
}

function isValueBearingOption(normalized: string): boolean {
  for (const [name, def] of Object.entries(GLOBAL_OPTIONS)) {
    if ((name === normalized || def.shortOption === normalized) && def.valueBearing) {
      return true
    }
  }
  return false
}

function isFlag(item: string): boolean {
  return item.startsWith('-')
}

function normalize(option: string): string {
  let index = 0
  while (index < option.length - 1 && option[index] === '-') index++
  return option.substring(index)
}

/**
 * Convert command args to a parameter map.
 * Mirrors Kotlin's toParameterMap.
 */
function toParameterMap(args: string[]): Record<string, string> {
  const parameters: Record<string, string> = {}
  let currentArg = 'default'
  for (const arg of args) {
    if (isFlag(arg)) {
      currentArg = normalize(arg)
    } else {
      parameters[currentArg] = arg
    }
  }
  return parameters
}

// ---------------------------------------------------------------------------
// Display functions (mirrors Kotlin's UserInteraction)
// ---------------------------------------------------------------------------

function printUsage(log: (...args: unknown[]) => void): void {
  log('SpecScript -- Create instantly runnable specs using Yaml and Markdown!')
  log('')
  log('Usage:\n   spec [global options] file | directory [command options]')
  log('\nGlobal options:')
  log(formatOptions(GLOBAL_OPTIONS))
}

function printDirectoryInfo(dirPath: string, log: (...args: unknown[]) => void): void {
  // Print description from specscript-config.yaml or README.md
  const description = getDirectoryDescription(dirPath)
  if (description) {
    log(description)
    log('')
  }

  // List available commands
  const commands = getDirectoryCommands(dirPath)
  if (commands.length === 0) {
    log('No commands available.')
  } else {
    log('Available commands:')
    const width = Math.max(...commands.map(c => c.name.length))
    for (const cmd of commands) {
      log(`  ${infoString(cmd.name, cmd.description, width)}`)
    }
  }
}

function printScriptInfo(filePath: string, log: (...args: unknown[]) => void): void {
  const content = readFileSync(filePath, 'utf-8')
  const script = Script.fromString(content)

  const info = getScriptInfo(script)
  if (info.description) log(info.description)

  if (info.properties && Object.keys(info.properties).length > 0) {
    if (info.description) log('')
    log('Options:')
    log(formatProperties(info.properties))
  }
}

// ---------------------------------------------------------------------------
// Script info extraction
// ---------------------------------------------------------------------------

interface ScriptInfoResult {
  description?: string
  hidden?: boolean
  properties?: Record<string, { description?: string; shortOption?: string }>
}

function getScriptInfo(script: Script): ScriptInfoResult {
  const result: ScriptInfoResult = {}

  for (const cmd of script.commands) {
    if (cmd.name === 'Script info') {
      if (typeof cmd.data === 'string') {
        result.description = cmd.data
      } else if (isObject(cmd.data)) {
        const obj = cmd.data as JsonObject
        if (typeof obj['description'] === 'string') result.description = obj['description']
        if (obj['hidden'] === true) result.hidden = true
      }
    }

    if (cmd.name === 'Input schema') {
      if (isObject(cmd.data)) {
        const schema = cmd.data as JsonObject
        const props = schema['properties']
        if (isObject(props)) {
          result.properties = extractProperties(props as JsonObject)
        }
      }
    }

    if (cmd.name === 'Input parameters') {
      if (isObject(cmd.data)) {
        result.properties = extractProperties(cmd.data as JsonObject)
      }
    }
  }

  return result
}

function extractProperties(props: JsonObject): Record<string, { description?: string; shortOption?: string }> {
  const result: Record<string, { description?: string; shortOption?: string }> = {}
  for (const [name, def] of Object.entries(props)) {
    if (isObject(def)) {
      const obj = def as JsonObject
      result[name] = {
        description: typeof obj['description'] === 'string' ? obj['description'] : undefined,
        shortOption: typeof obj['short option'] === 'string' ? obj['short option'] : undefined,
      }
    } else {
      result[name] = { description: typeof def === 'string' ? def : undefined }
    }
  }
  return result
}

// ---------------------------------------------------------------------------
// Directory info
// ---------------------------------------------------------------------------

interface CommandInfo {
  name: string
  description: string
}

function getDirectoryDescription(dirPath: string): string | undefined {
  // Try specscript-config.yaml first
  const configPath = resolve(dirPath, 'specscript-config.yaml')
  if (existsSync(configPath)) {
    try {
      const content = readFileSync(configPath, 'utf-8')
      const commands = parseYamlCommands(content)
      for (const cmd of commands) {
        if (cmd.name === 'Script info') {
          if (typeof cmd.data === 'string') return cmd.data
          if (isObject(cmd.data)) {
            const obj = cmd.data as JsonObject
            if (typeof obj['description'] === 'string') return obj['description']
          }
        }
      }
    } catch { /* ignore */ }
  }

  // Fall back to README.md
  const readmePath = resolve(dirPath, 'README.md')
  if (existsSync(readmePath)) {
    return extractDescriptionFromMarkdown(readFileSync(readmePath, 'utf-8'))
  }

  return undefined
}

/**
 * Recursively checks whether a directory (or any nested subdirectory) contains
 * at least one .spec.yaml or .spec.md file.
 */
function hasCliCommands(dirPath: string): boolean {
  for (const entry of readdirSync(dirPath)) {
    if (entry.endsWith('.spec.yaml') || entry.endsWith('.spec.md')) return true
    const full = resolve(dirPath, entry)
    try {
      if (statSync(full).isDirectory() && hasCliCommands(full)) return true
    } catch { /* skip unreadable entries */ }
  }
  return false
}

function getDirectoryCommands(dirPath: string): CommandInfo[] {
  const entries = readdirSync(dirPath)
  const commands: CommandInfo[] = []

  for (const entry of entries) {
    const filePath = resolve(dirPath, entry)

    // Spec files → file commands
    if (entry.endsWith('.spec.yaml') || entry.endsWith('.spec.md')) {
      if (entry.endsWith('.spec.yaml')) {
        try {
          const content = readFileSync(filePath, 'utf-8')
          const script = Script.fromString(content)
          const info = getScriptInfo(script)
          if (info.hidden) continue
        } catch { /* include if we can't parse */ }
      }

      const name = stripSpecExtension(entry).replace(/ /g, '-').toLowerCase()
      const description = getScriptDescription(filePath, entry)
      commands.push({ name, description })
      continue
    }

    // Subdirectories → directory commands (skip "tests" directory)
    if (entry === 'tests') continue
    try {
      if (statSync(filePath).isDirectory() && hasCliCommands(filePath)) {
        const name = entry.replace(/ /g, '-').toLowerCase()
        const description = getDirectoryDescription(filePath) ?? ''
        commands.push({ name, description })
      }
    } catch { /* skip unreadable entries */ }
  }

  commands.sort((a, b) => a.name.localeCompare(b.name))
  return commands
}

function getScriptDescription(filePath: string, fileName: string): string {
  try {
    const content = readFileSync(filePath, 'utf-8')

    if (filePath.endsWith('.spec.md')) {
      return extractDescriptionFromMarkdown(content) ?? stripSpecExtension(fileName)
    }

    const script = Script.fromString(content)
    const info = getScriptInfo(script)
    if (info.description) return info.description
  } catch { /* fall through */ }

  return stripSpecExtension(fileName).replace(/-/g, ' ')
}

function extractDescriptionFromMarkdown(content: string): string | undefined {
  const lines = content.split('\n')
  let foundHeading = false
  const descriptionLines: string[] = []

  for (const line of lines) {
    if (!foundHeading) {
      if (line.startsWith('# ')) foundHeading = true
      continue
    }
    const trimmed = line.trim()
    if (trimmed === '') {
      if (descriptionLines.length > 0) break
      continue
    }
    descriptionLines.push(trimmed)
  }

  return descriptionLines.length > 0 ? descriptionLines.join(' ') : undefined
}

function stripSpecExtension(name: string): string {
  if (name.endsWith('.spec.yaml')) return name.slice(0, -'.spec.yaml'.length)
  if (name.endsWith('.spec.md')) return name.slice(0, -'.spec.md'.length)
  return name
}

// ---------------------------------------------------------------------------
// Core CLI logic
// ---------------------------------------------------------------------------

/**
 * CLI entry point. Registers commands and translates the exit code to process.exit.
 */
async function main(): Promise<void> {
  registerAllCommands()
  const args = process.argv.slice(2)
  const code = await runCli(args, process.cwd())
  if (code !== 0) process.exit(code)
}

/**
 * Core CLI logic. Returns an exit code (0 = success).
 * Designed for in-process invocation — never calls process.exit().
 * Mirrors Kotlin's SpecScriptCli.main(args, workingDir).
 */
export async function runCli(
  args: string[],
  workingDir: string,
  log: (...args: unknown[]) => void = console.log,
  logError: (...args: unknown[]) => void = console.error,
): Promise<number> {

  let options: CliOptions
  try {
    options = parseCliOptions(args)
  } catch (e) {
    if (e instanceof CliInvocationError) {
      logError(e.message)
      return 1
    }
    throw e
  }

  // Set package path before command dispatch (resolve relative to workingDir)
  setPackagePath(options.packagePath ? resolve(workingDir, options.packagePath) : undefined)

  // No commands → print usage
  if (options.commands.length === 0) {
    printUsage(log)
    return 0
  }

  try {
    // Resolve command
    const command = options.commands[0]
    const resolvedPath = resolveCommand(command, workingDir)

    if (resolvedPath === undefined) {
      throw new CliInvocationError(`Could not find spec file for: ${command}`)
    }

    const stat = statSync(resolvedPath)

    if (options.testMode) {
      await runTests(resolvedPath, stat.isDirectory(), log)
      return 0
    }

    if (stat.isDirectory()) {
      // Directory invocation
      await invokeDirectory(resolvedPath, options.commands.slice(1), options, log, logError)
    } else {
      // File invocation
      await invokeFile(resolvedPath, options, log, logError)
    }
  } catch (e) {
    if (e instanceof CliInvocationError) {
      logError(e.message)
      return 1
    }
    if (e instanceof MissingInputError) {
      logError(`Missing parameter: --${e.parameterName}`)
      // TODO: print options list when we have the script info available
      return 1
    }
    if (e instanceof SpecScriptCommandError) {
      reportCommandError(e, logError)
      return 1
    }
    if (e instanceof SpecScriptError) {
      reportLanguageError(e, options.debug, logError)
      return 1
    }
    throw e
  }

  return 0
}

async function invokeFile(
  filePath: string,
  options: CliOptions,
  log: (...args: unknown[]) => void,
  logError: (...args: unknown[]) => void,
): Promise<void> {
  // Help mode
  if (options.help) {
    printScriptInfo(filePath, log)
    return
  }

  // Execute file with parameters
  const parameters = toParameterMap(options.commandArgs)
  const result = await executeFile(filePath, undefined, parameters, log, options.interactive)

  // Print output
  if (result !== undefined && result !== null) {
    if (options.printOutput === 'json') {
      const json = JSON.stringify(result, null, 2)
      log(json)
    } else if (options.printOutput === 'yaml') {
      const yaml = toDisplayYaml(result)
      if (yaml.trim()) log(yaml)
    }
  }
}

async function invokeDirectory(
  dirPath: string,
  subcommands: string[],
  options: CliOptions,
  log: (...args: unknown[]) => void,
  logError: (...args: unknown[]) => void,
): Promise<void> {
  // No subcommand
  if (subcommands.length === 0) {
    // Always print directory description first (matches Kotlin: printDirectoryInfo before command selection)
    const description = getDirectoryDescription(dirPath)
    if (description) {
      log(description)
      log('')
    }

    const commands = getDirectoryCommands(dirPath)

    // Interactive mode — show selection menu
    if (options.interactive && !options.help) {
      if (commands.length === 0) {
        log('No commands available.')
        return
      }

      const { select } = await import('@inquirer/prompts')
      const width = Math.max(...commands.map(c => c.name.length))
      const selected = await select({
        message: 'Available commands:',
        choices: commands.map(cmd => ({
          name: infoString(cmd.name, cmd.description, width),
          value: cmd.name,
        })),
      })

      // Resolve and execute selected command
      const resolvedPath = resolveCommand(selected, dirPath)
      if (resolvedPath === undefined) {
        throw new CliInvocationError(`Command '${selected}' not found in ${basename(dirPath)}`)
      }
      const stat = statSync(resolvedPath)
      if (stat.isDirectory()) {
        await invokeDirectory(resolvedPath, [], options, log, logError)
      } else {
        await invokeFile(resolvedPath, options, log, logError)
      }
      return
    }

    // Non-interactive — print commands listing
    if (commands.length === 0) {
      log('No commands available.')
    } else {
      log('Available commands:')
      const width = Math.max(...commands.map(c => c.name.length))
      for (const cmd of commands) {
        log(`  ${infoString(cmd.name, cmd.description, width)}`)
      }
    }
    return
  }

  // Resolve subcommand
  const subcommand = subcommands[0]
  const resolvedPath = resolveCommand(subcommand, dirPath)

  if (resolvedPath === undefined) {
    throw new CliInvocationError(`Command '${subcommand}' not found in ${basename(dirPath)}`)
  }

  const stat = statSync(resolvedPath)
  if (stat.isDirectory()) {
    await invokeDirectory(resolvedPath, subcommands.slice(1), options, log, logError)
  } else {
    await invokeFile(resolvedPath, options, log, logError)
  }
}

// ---------------------------------------------------------------------------
// File resolution and execution
// ---------------------------------------------------------------------------

/**
 * Resolve a command name to a file or directory path.
 * Tries: exact match → .spec.yaml → .spec.md
 */
export function resolveCommand(command: string, workingDir: string): string | undefined {
  const exact = resolve(workingDir, command)
  if (existsSync(exact)) return exact

  const yaml = resolve(workingDir, `${command}.spec.yaml`)
  if (existsSync(yaml)) return yaml

  const md = resolve(workingDir, `${command}.spec.md`)
  if (existsSync(md)) return md

  return undefined
}

/**
 * Execute a spec file (yaml or md). When a parent context is provided,
 * the new context shares its session (for stdout capture) but gets fresh variables.
 *
 * @param log - optional output function for Print commands. When omitted, defaults to console.log.
 *              Used by in-process Cli command to capture output without monkey-patching console.
 */
export async function executeFile(
  filePath: string,
  parent?: ScriptContext,
  inputParameters?: Record<string, string>,
  log?: (...args: unknown[]) => void,
  interactive?: boolean,
): Promise<JsonValue | undefined> {
  const content = readFileSync(filePath, 'utf-8')
  const workingDir = parent?.workingDir ?? dirname(filePath)

  const context = new DefaultContext({
    scriptFile: filePath,
    workingDir,
    session: parent?.session,
    interactive: interactive ?? parent?.interactive ?? false,
  })

  if (!parent) {
    setupStdoutCapture(context, log)
  }

  // Inject command-line parameters into the input variable
  if (inputParameters && Object.keys(inputParameters).length > 0) {
    const input = (context.variables.get('input') as JsonObject) ?? {}
    for (const [key, value] of Object.entries(inputParameters)) {
      if (key !== 'default') input[key] = value
    }
    context.variables.set('input', input)
  }

  if (filePath.endsWith('.spec.md')) {
    const blocks = scanMarkdown(content)
    const scripts = splitMarkdownSections(blocks)

    for (const script of scripts) {
      if (script.commands.length === 0) continue
      const captured = context.session.get('capturedOutput') as string[] | undefined
      if (captured) captured.length = 0
      await script.run(context)
    }
  } else {
    const script = Script.fromString(content)
    await script.run(context)
  }

  return context.output
}

// ---------------------------------------------------------------------------
// Test mode (--test)
// ---------------------------------------------------------------------------

interface TestReport {
  details: Array<{ testCase: string; error: string }>
  failed: number
  passed: number
}

async function runTests(
  path: string,
  isDirectory: boolean,
  log: (...args: unknown[]) => void,
): Promise<void> {
  const report: TestReport = { details: [], failed: 0, passed: 0 }

  if (isDirectory) {
    await runTestsInDirectory(path, report, log)
  } else {
    await runTestsInFile(path, report, log)
  }

  log(toDisplayYaml(report))
}

async function runTestsInDirectory(
  dirPath: string,
  report: TestReport,
  log: (...args: unknown[]) => void,
): Promise<void> {
  const entries = readdirSync(dirPath).sort()
  for (const entry of entries) {
    const fullPath = resolve(dirPath, entry)
    const stat = statSync(fullPath)
    if (stat.isDirectory()) {
      await runTestsInDirectory(fullPath, report, log)
    } else if (entry.endsWith('.spec.yaml') || entry.endsWith('.spec.md')) {
      await runTestsInFile(fullPath, report, log)
    }
  }
}

async function runTestsInFile(
  filePath: string,
  report: TestReport,
  log: (...args: unknown[]) => void,
): Promise<void> {
  const content = readFileSync(filePath, 'utf-8')

  if (filePath.endsWith('.spec.md')) {
    await runMarkdownTests(filePath, content, report, log)
  } else {
    await runYamlTests(filePath, content, report, log)
  }
}

async function runYamlTests(
  filePath: string,
  content: string,
  report: TestReport,
  log: (...args: unknown[]) => void,
): Promise<void> {
  const script = Script.fromString(content)

  // Check for new-style Tests command first
  const hasNewTests = script.commands.some(c => c.name.toLowerCase() === 'tests')
  const hasLegacyTests = script.commands.some(c => c.name.toLowerCase() === 'test case')

  if (!hasNewTests && !hasLegacyTests) return

  const context = new DefaultContext({
    scriptFile: filePath,
    workingDir: dirname(filePath),
  })
  setupStdoutCapture(context, log)

  if (hasNewTests) {
    const suite = script.splitTests()

    // Convert tests script into named tests (each command = one test)
    const testEntries: Array<{ name: string; commands: Command[] }> = []
    for (const cmd of suite.tests.commands) {
      const testCommands: Command[] = []
      // Each key in the Tests object is a test name, value is the test body
      if (isObject(cmd.data)) {
        for (const [key, value] of Object.entries(cmd.data as Record<string, JsonValue>)) {
          testCommands.push({ name: key, data: value })
        }
      }
      testEntries.push({ name: cmd.name, commands: testCommands.length > 0 ? testCommands : [cmd] })
    }

    for (let i = 0; i < testEntries.length; i++) {
      const test = testEntries[i]
      const commands: Command[] = []

      // Prepend setup to first test
      if (i === 0 && suite.setup.commands.length > 0) {
        commands.push(...suite.setup.commands)
      }
      commands.push(...test.commands)
      // Append teardown to last test
      if (i === testEntries.length - 1 && suite.teardown.commands.length > 0) {
        commands.push(...suite.teardown.commands)
      }

      await runSingleTest(test.name, new Script(commands), context, report)
    }
  } else {
    // Legacy Test case
    const testCases = script.splitTestCases()
    for (const tc of testCases) {
      await runSingleTest(tc.name, tc.script, context, report)
    }
  }
}

async function runMarkdownTests(
  filePath: string,
  content: string,
  report: TestReport,
  log: (...args: unknown[]) => void,
): Promise<void> {
  const blocks = scanMarkdown(content)
  const scripts = splitMarkdownSections(blocks)

  const context = new DefaultContext({
    scriptFile: filePath,
    workingDir: dirname(filePath),
  })
  setupStdoutCapture(context, log)

  for (const script of scripts) {
    if (script.commands.length === 0) continue

    const name = script.title ?? getTestNameFromScript(script) ?? 'Untitled'
    await runSingleTest(name, script, context, report)
  }
}

function getTestNameFromScript(script: Script): string | undefined {
  for (const cmd of script.commands) {
    if (cmd.name.toLowerCase() === 'code example' && typeof cmd.data === 'string') {
      return cmd.data
    }
  }
  return undefined
}

async function runSingleTest(
  name: string,
  script: Script,
  context: ScriptContext,
  report: TestReport,
): Promise<void> {
  // Reset state before each test
  context.error = undefined
  context.variables.delete('input')
  const captured = context.session.get('capturedOutput') as string[] | undefined
  if (captured) captured.length = 0

  try {
    await script.run(context)
    report.passed++
  } catch (e) {
    report.failed++
    const message = e instanceof Error ? e.message : String(e)
    report.details.push({ testCase: name, error: message })
  }
}

// ---------------------------------------------------------------------------
// Error reporting (mirrors Kotlin's CliErrorReporter)
// ---------------------------------------------------------------------------

function reportLanguageError(e: SpecScriptError, debug: boolean, logError: (...args: unknown[]) => void): void {
  logError('\nScripting error')

  if (e.cause === null || e.cause === undefined || e.cause instanceof SpecScriptError) {
    logError(`\n${e.message}`)
  } else {
    if (debug) {
      logError(`\nCaused by: ${e.cause}`)
      if (e.cause instanceof Error && e.cause.stack) {
        logError(e.cause.stack)
      }
    } else {
      logError(`\nCaused by: ${e.cause}`)
    }
  }
}

function reportCommandError(e: SpecScriptCommandError, logError: (...args: unknown[]) => void): void {
  logError(e.message)
}

// ---------------------------------------------------------------------------
// Main module check
// ---------------------------------------------------------------------------

const isMainModule = process.argv[1] && resolve(process.argv[1]).includes('cli')
if (isMainModule) {
  main()
}
