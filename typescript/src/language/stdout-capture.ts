import type { ScriptContext } from './context.js'

/**
 * Set up stdout capture on a context.
 * Print commands will write to the captured buffer instead of directly to console.
 * ExpectedConsoleOutput reads from the buffer.
 */
export function setupStdoutCapture(context: ScriptContext): void {
  const captured: string[] = []
  context.session.set('capturedOutput', captured)

  context.session.set('stdout', (text: string) => {
    captured.push(text)
    console.log(text)
  })
}

/**
 * Set up stdout capture that only captures (no console output).
 * Used for testing.
 */
export function setupSilentCapture(context: ScriptContext): void {
  const captured: string[] = []
  context.session.set('capturedOutput', captured)

  context.session.set('stdout', (text: string) => {
    captured.push(text)
  })
}
