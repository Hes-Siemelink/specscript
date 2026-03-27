/**
 * Cli command: invoke the SpecScript CLI in-process.
 *
 * Delegates to cli.ts for command resolution and file execution,
 * avoiding duplication of the core CLI logic.
 */

import { readFileSync, existsSync, readdirSync, statSync } from 'node:fs'
import { resolve } from 'node:path'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue } from '../language/types.js'
import { isObject, isString, CommandFormatError } from '../language/types.js'
import { resolveCommand, executeFile } from '../cli.js'
import { parseYamlCommands } from '../util/yaml.js'

export const CliCommand: CommandHandler = {
  name: 'Cli',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isString(data)) {
      return runCli(context, data)
    }

    if (isObject(data)) {
      const command = isString(data.command) ? data.command : undefined
      if (!command) {
        throw new CommandFormatError("Cli: missing required 'command' parameter")
      }
      const cd = isString(data.cd) ? data.cd : undefined
      // cd is resolved relative to workingDir
      const workingDir = cd ? resolve(context.workingDir, cd) : context.workingDir
      return runCli(context, command, workingDir)
    }

    throw new CommandFormatError('Cli: expected command string or object')
  },
}

async function runCli(context: ScriptContext, command: string, workingDir?: string): Promise<JsonValue> {
  const dir = workingDir ?? context.workingDir

  // Parse command: drop first token if it's "spec"
  const args = command.split(/\s+/)
  if (args[0] === 'spec') {
    args.shift()
  }

  const { stdout, stderr } = await runCliInProcess(args, dir, context)

  // Combine stdout and stderr
  const parts = [stdout, stderr].filter(s => s)
  const combined = parts.join('\n')

  // Write to session capture for ExpectedConsoleOutput
  const stdoutFn = context.session.get('stdout') as ((text: string) => void) | undefined
  if (stdoutFn && combined) {
    stdoutFn(combined)
  }

  return combined
}

/**
 * Run the CLI in-process, capturing stdout/stderr.
 * Uses shared resolveCommand and executeFile from cli.ts.
 */
async function runCliInProcess(
  args: string[],
  workingDir: string,
  parentContext: ScriptContext
): Promise<{ stdout: string; stderr: string }> {
  const stdoutLines: string[] = []
  const stderrLines: string[] = []

  const origLog = console.log
  const origError = console.error
  console.log = (...logArgs: unknown[]) => { stdoutLines.push(logArgs.map(String).join(' ')) }
  console.error = (...logArgs: unknown[]) => { stderrLines.push(logArgs.map(String).join(' ')) }

  try {
    // Split into global flags and command tokens
    const globalFlags: string[] = []
    const commands: string[] = []
    let parsingGlobals = true
    for (const arg of args) {
      if (parsingGlobals && arg.startsWith('-')) {
        globalFlags.push(arg.replace(/^-+/, ''))
      } else {
        parsingGlobals = false
        commands.push(arg)
      }
    }

    const isHelp = globalFlags.includes('help') || globalFlags.includes('h')

    // No commands and --help → print usage
    if (commands.length === 0) {
      if (isHelp || args.length === 0) {
        printUsage(stdoutLines)
        return cliResult(stdoutLines, stderrLines)
      }
    }

    // Resolve command path (shared with cli.ts)
    const commandName = commands[0]
    const resolvedPath = resolveCommand(commandName, workingDir)

    if (resolvedPath === undefined) {
      stderrLines.push(`Could not find spec file for: ${commandName}`)
      return cliResult(stdoutLines, stderrLines)
    }

    // Directory → list available commands
    const stat = statSync(resolvedPath)
    if (stat.isDirectory()) {
      printDirectoryInfo(resolvedPath, stdoutLines)
      return cliResult(stdoutLines, stderrLines)
    }

    // Execute file (shared with cli.ts)
    await executeFile(resolvedPath, parentContext)
  } catch (e) {
    if (e instanceof Error) {
      stderrLines.push(`Error: ${e.message}`)
    } else {
      stderrLines.push(`Error: ${e}`)
    }
  } finally {
    console.log = origLog
    console.error = origError
  }

  return cliResult(stdoutLines, stderrLines)
}

function cliResult(stdoutLines: string[], stderrLines: string[]): { stdout: string; stderr: string } {
  return {
    stdout: stdoutLines.length > 0 ? stdoutLines.join('\n') + '\n' : '',
    stderr: stderrLines.length > 0 ? stderrLines.join('\n') + '\n' : '',
  }
}

// --- CLI display helpers (not shared — only needed for in-process invocation) ---

function printUsage(lines: string[]): void {
  lines.push('SpecScript -- Create instantly runnable specs using Yaml and Markdown!')
  lines.push('')
  lines.push('Usage:')
  lines.push('   spec [global options] file | directory [command options]')
  lines.push('')
  lines.push('Global options:')
  lines.push('  --help, -h      Print help on a script or directory and does not run anything')
  lines.push('  --output, -o    Print the output at the end of the script in Yaml format')
  lines.push('  --output-json, -j   Print the output at the end of the script in Json format')
  lines.push('  --interactive, -i   SpecScript may prompt for user input if it needs more information')
  lines.push('  --debug, -d     Run in debug mode. Prints stacktraces when an error occurs.')
  lines.push('  --test, -t      Run in test mode. Only tests will be executed.')
}

function printDirectoryInfo(dirPath: string, lines: string[]): void {
  const readmePath = resolve(dirPath, 'README.md')
  if (existsSync(readmePath)) {
    const readmeContent = readFileSync(readmePath, 'utf-8')
    const description = extractDescriptionFromMarkdown(readmeContent)
    if (description) {
      lines.push(description)
      lines.push('')
    }
  }

  const entries = readdirSync(dirPath)
  const commands: { name: string; description: string }[] = []

  for (const entry of entries) {
    if (entry.endsWith('.spec.yaml') || entry.endsWith('.spec.md')) {
      const name = stripSpecExtension(entry).replace(/ /g, '-').toLowerCase()
      const filePath = resolve(dirPath, entry)
      const description = getScriptDescription(filePath, entry)
      commands.push({ name, description })
    }
  }

  commands.sort((a, b) => a.name.localeCompare(b.name))

  if (commands.length === 0) {
    lines.push('No commands available.')
  } else {
    lines.push('Available commands:')
    const maxWidth = Math.max(...commands.map(c => c.name.length))
    for (const cmd of commands) {
      lines.push(`  ${cmd.name.padEnd(maxWidth)}   ${cmd.description}`)
    }
  }
}

function extractDescriptionFromMarkdown(content: string): string {
  const markdownLines = content.split('\n')
  let foundHeading = false
  const descriptionLines: string[] = []

  for (const line of markdownLines) {
    if (!foundHeading) {
      if (line.startsWith('# ')) {
        foundHeading = true
      }
      continue
    }

    const trimmed = line.trim()
    if (trimmed === '') {
      if (descriptionLines.length > 0) break
      continue
    }
    descriptionLines.push(trimmed)
  }

  return descriptionLines.join(' ')
}

function getScriptDescription(filePath: string, fileName: string): string {
  try {
    const content = readFileSync(filePath, 'utf-8')

    if (filePath.endsWith('.spec.md')) {
      return extractDescriptionFromMarkdown(content)
    }

    const commands = parseYamlCommands(content)
    for (const cmd of commands) {
      if (cmd.name === 'Script info') {
        if (typeof cmd.data === 'string') {
          return cmd.data
        }
        if (typeof cmd.data === 'object' && cmd.data !== null && !Array.isArray(cmd.data)) {
          const obj = cmd.data as Record<string, unknown>
          if ('description' in obj) {
            return String(obj.description)
          }
        }
      }
    }
  } catch {
    // Fall through to default
  }

  return stripSpecExtension(fileName).replace(/-/g, ' ')
}

function stripSpecExtension(name: string): string {
  if (name.endsWith('.spec.yaml')) return name.slice(0, -'.spec.yaml'.length)
  if (name.endsWith('.spec.md')) return name.slice(0, -'.spec.md'.length)
  return name
}
