import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const pendingCleanupDirs: Set<string> = new Set()
let cleanupRegistered = false

/**
 * Create a temp directory under the OS temp dir, registered for best-effort cleanup on
 * process exit. Doesn't fire on SIGINT or an uncaught rejection, matching the Kotlin
 * implementation's File.deleteOnExit limitation.
 */
export function createTempDir(): string {
  const dir = mkdtempSync(join(tmpdir(), 'specscript-'))
  registerCleanup(dir)
  return dir
}

function registerCleanup(dir: string): void {
  pendingCleanupDirs.add(dir)
  if (!cleanupRegistered) {
    cleanupRegistered = true
    process.on('exit', () => {
      for (const d of pendingCleanupDirs) {
        try {
          rmSync(d, { recursive: true, force: true })
        } catch {
          // Best effort
        }
      }
    })
  }
}
