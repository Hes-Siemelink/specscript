#!/usr/bin/env node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { Script } from './language/script.js'
import { DefaultContext } from './language/context.js'
import { registerAllCommands } from './commands/register.js'
import { setupStdoutCapture } from './language/stdout-capture.js'
import { scanMarkdown } from './markdown/scanner.js'
import { splitMarkdownSections, getTestTitle } from './markdown/converter.js'
import type { SpecScriptCommandError } from './language/types.js'

registerAllCommands()

function main(): void {
  const args = process.argv.slice(2)

  if (args.length === 0) {
    console.error('Usage: spec-ts <file.spec.yaml|file.spec.md>')
    process.exit(1)
  }

  const filePath = resolve(args[0])
  const content = readFileSync(filePath, 'utf-8')

  if (filePath.endsWith('.spec.md')) {
    runMarkdown(filePath, content)
  } else {
    runYaml(filePath, content)
  }
}

function runYaml(filePath: string, content: string): void {
  const script = Script.fromString(content)
  const context = new DefaultContext({ scriptFile: filePath })
  setupStdoutCapture(context)

  try {
    script.run(context)
  } catch (e) {
    reportError(e)
    process.exit(1)
  }
}

function runMarkdown(filePath: string, content: string): void {
  const blocks = scanMarkdown(content)
  const scripts = splitMarkdownSections(blocks)
  const context = new DefaultContext({ scriptFile: filePath })
  setupStdoutCapture(context)

  for (const script of scripts) {
    if (script.commands.length === 0) continue

    // Reset captured output before each section
    const captured = context.session.get('capturedOutput') as string[] | undefined
    if (captured) captured.length = 0

    try {
      script.run(context)
    } catch (e) {
      reportError(e)
      process.exit(1)
    }
  }
}

function reportError(e: unknown): void {
  if (e instanceof Error) {
    console.error(`Error: ${e.message}`)
  } else {
    console.error(`Error: ${e}`)
  }
}

main()
