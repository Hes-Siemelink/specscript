/**
 * Shell command: execute shell commands via /bin/bash.
 */

import { execSync } from 'node:child_process'
import { resolve } from 'node:path'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isString, CommandFormatError, SpecScriptCommandError, toDisplayString } from '../language/types.js'

export const ShellCommand: CommandHandler = {
  name: 'Shell',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    // Reset expected console output
    resetExpectedConsoleOutput(context)

    if (isString(data)) {
      return executeShell(data, context.workingDir, buildEnv(context), {}, context)
    }

    if (isObject(data)) {
      const command = isString(data.command) ? data.command : undefined
      const resource = isString(data.resource) ? data.resource : undefined
      const cd = isString(data.cd) ? data.cd : undefined
      const showOutput = data['show output'] === true
      const showCommand = data['show command'] === true
      const captureOutput = data['capture output'] !== false  // default true

      const commandLine = command ?? resource
      if (!commandLine) {
        throw new CommandFormatError("Specify shell command in either 'command' or 'resource' property")
      }

      let workDir: string
      if (cd) {
        workDir = cd
      } else if (command) {
        workDir = context.workingDir
      } else {
        // resource: resolve from scriptDir
        workDir = context.scriptDir
      }

      // Merge additional env vars from data
      const env = buildEnv(context)
      if (isObject(data.env)) {
        for (const [key, value] of Object.entries(data.env)) {
          env[key] = toDisplayString(value)
        }
      }

      return executeShell(commandLine, workDir, env, { showOutput, showCommand, captureOutput }, context)
    }

    throw new CommandFormatError('Shell: expected command string or object')
  },
}

function buildEnv(context: ScriptContext): Record<string, string> {
  const env: Record<string, string> = { ...process.env as Record<string, string> }

  // Expose script variables as env vars
  for (const [key, value] of context.variables) {
    env[key] = toDisplayString(value)
  }

  // Expose SCRIPT_HOME
  env['SCRIPT_HOME'] = context.scriptHome

  return env
}

/** Write to the session's stdout capture (for show output / show command). */
function writeToCapture(context: ScriptContext, text: string): void {
  const stdoutFn = context.session.get('stdout') as ((text: string) => void) | undefined
  if (stdoutFn) {
    stdoutFn(text)
  } else {
    console.log(text)
  }
}

function executeShell(
  commandLine: string,
  workingDir: string,
  env: Record<string, string>,
  options: { showOutput?: boolean; showCommand?: boolean; captureOutput?: boolean },
  context: ScriptContext
): JsonValue | undefined {
  const { showOutput = false, showCommand = false, captureOutput = true } = options

  if (showCommand) {
    writeToCapture(context, commandLine)
  }

  try {
    const result = execSync(commandLine, {
      cwd: workingDir,
      env,
      encoding: 'utf-8',
      stdio: ['pipe', 'pipe', 'pipe'],
      shell: '/bin/bash',
    })

    // Merge stdout (result is stdout since we set encoding)
    const output = result.trim()

    if (showOutput && output) {
      writeToCapture(context, output)
    }

    return captureOutput ? output : undefined
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'status' in e) {
      const execError = e as { status: number; stdout?: string; stderr?: string }
      const stdout = typeof execError.stdout === 'string' ? execError.stdout : ''
      const stderr = typeof execError.stderr === 'string' ? execError.stderr : ''
      const combined = (stdout + stderr).trim()

      if (showOutput && combined) {
        writeToCapture(context, combined)
      }

      throw new SpecScriptCommandError(
        'Shell command failed',
        'shell',
        { exitCode: String(execError.status) }
      )
    }
    throw new SpecScriptCommandError(
      `Shell error: ${e instanceof Error ? e.message : String(e)}`,
      'shell'
    )
  }
}

/**
 * Reset the expected console output counter before shell execution.
 */
function resetExpectedConsoleOutput(context: ScriptContext): void {
  const captured = context.session.get('capturedOutput') as string[] | undefined
  if (captured) captured.length = 0
}
