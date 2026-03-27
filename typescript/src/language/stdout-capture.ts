import type { ScriptContext } from './context.js'

/**
 * Set up stdout capture on a context.
 * Print commands will write to the captured buffer and also to the provided output function.
 * ExpectedConsoleOutput reads from the buffer.
 *
 * @param log - output function, defaults to console.log. In-process Cli command passes a
 *              capture function here so Print output doesn't leak to the real console.
 */
export function setupStdoutCapture(context: ScriptContext, log?: (...args: unknown[]) => void): void {
  const captured: string[] = []
  context.session.set('capturedOutput', captured)

  const out = log ?? console.log
  context.session.set('stdout', (text: string) => {
    captured.push(text)
    out(text)
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
