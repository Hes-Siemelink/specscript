#!/usr/bin/env node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { Script } from './language/script.js'
import { DefaultContext } from './language/context.js'
import { registerLevel0Commands } from './commands/register.js'
import { setupStdoutCapture } from './language/stdout-capture.js'

// Register all Level 0 commands
registerLevel0Commands()

function main(): void {
  const args = process.argv.slice(2)

  if (args.length === 0) {
    console.error('Usage: specscript-ts <file.spec.yaml>')
    process.exit(1)
  }

  const filePath = resolve(args[0])
  const content = readFileSync(filePath, 'utf-8')

  const script = Script.fromString(content)
  const context = new DefaultContext({ scriptFile: filePath })

  // Set up stdout capture for Expected console output
  setupStdoutCapture(context)

  try {
    script.run(context)
  } catch (e) {
    if (e instanceof Error) {
      console.error(`Error: ${e.message}`)
    } else {
      console.error(`Error: ${e}`)
    }
    process.exit(1)
  }
}

main()
