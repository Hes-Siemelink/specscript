#!/usr/bin/env node

import { readFileSync, existsSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { Script } from './language/script.js'
import { DefaultContext } from './language/context.js'
import type { ScriptContext } from './language/context.js'
import { registerAllCommands } from './commands/register.js'
import { setupStdoutCapture } from './language/stdout-capture.js'
import { scanMarkdown } from './markdown/scanner.js'
import { splitMarkdownSections } from './markdown/converter.js'

/**
 * CLI entry point. Registers commands and translates the exit code to process.exit.
 */
function main(): void {
  registerAllCommands()
  const args = process.argv.slice(2)
  const code = runCli(args, process.cwd())
  if (code !== 0) process.exit(code)
}

/**
 * Core CLI logic. Returns an exit code (0 = success).
 * Designed for in-process invocation — never calls process.exit().
 * Mirrors Kotlin's SpecScriptCli.main(args, workingDir).
 */
export function runCli(args: string[], workingDir: string): number {
  if (args.length === 0) {
    console.error('Usage: spec-ts <file.spec.yaml|file.spec.md>')
    return 1
  }

  const filePath = resolve(workingDir, args[0])

  if (!existsSync(filePath)) {
    console.error(`File not found: ${filePath}`)
    return 1
  }

  try {
    executeFile(filePath)
  } catch (e) {
    reportError(e)
    return 1
  }

  return 0
}

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
 */
export function executeFile(filePath: string, parent?: ScriptContext): void {
  const content = readFileSync(filePath, 'utf-8')
  const workingDir = parent?.workingDir ?? dirname(filePath)

  const context = new DefaultContext({
    scriptFile: filePath,
    workingDir,
    session: parent?.session,
  })

  if (!parent) {
    setupStdoutCapture(context)
  }

  if (filePath.endsWith('.spec.md')) {
    const blocks = scanMarkdown(content)
    const scripts = splitMarkdownSections(blocks)

    for (const script of scripts) {
      if (script.commands.length === 0) continue
      const captured = context.session.get('capturedOutput') as string[] | undefined
      if (captured) captured.length = 0
      script.run(context)
    }
  } else {
    const script = Script.fromString(content)
    script.run(context)
  }
}

function reportError(e: unknown): void {
  if (e instanceof Error) {
    console.error(`Error: ${e.message}`)
  } else {
    console.error(`Error: ${e}`)
  }
}

// Only run main when executed directly (not imported)
const isMainModule = process.argv[1] && resolve(process.argv[1]).includes('cli')
if (isMainModule) {
  main()
}
