/**
 * Cli command: invoke the SpecScript CLI in-process.
 *
 * Delegates entirely to runCli from cli.ts, capturing stdout/stderr
 * via log callbacks instead of monkey-patching console.
 */

import { resolve } from 'node:path'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue } from '../language/types.js'
import { isObject, isString, CommandFormatError } from '../language/types.js'
import { runCli } from '../cli.js'

export const CliCommand: CommandHandler = {
  name: 'Cli',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isString(data)) {
      return invokeCli(context, data)
    }

    if (isObject(data)) {
      const command = isString(data.command) ? data.command : undefined
      if (!command) {
        throw new CommandFormatError("Cli: missing required 'command' parameter")
      }
      const cd = isString(data.cd) ? data.cd : undefined
      const workingDir = cd ? resolve(context.workingDir, cd) : context.workingDir
      return invokeCli(context, command, workingDir)
    }

    throw new CommandFormatError('Cli: expected command string or object')
  },
}

async function invokeCli(context: ScriptContext, command: string, workingDir?: string): Promise<JsonValue> {
  const dir = workingDir ?? context.workingDir

  // Parse command string into args, dropping leading "spec" token
  const args = command.split(/\s+/)
  if (args[0] === 'spec') {
    args.shift()
  }

  // Capture stdout and stderr via log callbacks
  const stdoutLines: string[] = []
  const stderrLines: string[] = []

  const log = (...logArgs: unknown[]) => { stdoutLines.push(logArgs.map(String).join(' ')) }
  const logError = (...logArgs: unknown[]) => { stderrLines.push(logArgs.map(String).join(' ')) }

  await runCli(args, dir, log, logError)

  // Build combined output (stdout first, then stderr)
  const stdout = stdoutLines.length > 0 ? stdoutLines.join('\n') + '\n' : ''
  const stderr = stderrLines.length > 0 ? stderrLines.join('\n') + '\n' : ''
  const parts = [stdout, stderr].filter(s => s)
  const combined = parts.join('\n')

  // Write to session capture for ExpectedConsoleOutput
  const stdoutFn = context.session.get('stdout') as ((text: string) => void) | undefined
  if (stdoutFn && combined) {
    stdoutFn(combined)
  }

  return combined
}
