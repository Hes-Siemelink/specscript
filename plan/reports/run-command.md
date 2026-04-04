# Report: Run command (replaces Run script)

Bean: specscript-hvrq

## Test results

- **Kotlin**: full `check` passes (unit + 554 specification tests)
- **TypeScript**: 481 passed, 13 skipped, 0 failed

## Architecture decisions

**Isolation ladder codified.** `Do` → `Run` → `Cli` is now documented in `Running SpecScript from SpecScript.spec.md`. `Run` sits in the middle: variable isolation + parent command delegation, without the full CLI re-invocation of `Cli`.

**`parentCommandLookup` as a simple field.** Rather than introducing a chain-of-responsibility pattern, `FileContext` (Kotlin) and `DefaultContext` (TypeScript) both got a nullable `parentCommandLookup` function. When an inline script can't find a command locally, it calls up. One field, no new interfaces.

**`DelayedResolver` for inline scripts.** The `Run` command's `execute` method selectively resolves metadata (`cd`, `file`, `script` when string, `input`) but leaves inline script bodies unresolved. This prevents the parent context from expanding `${variables}` that should only exist in the child scope. Both implementations handle this identically.

**`resource` dropped cold.** No alias, no migration path. Every reference in specs and samples was updated in the same commit. This keeps the codebase clean but means anyone with external scripts using `resource:` will break on upgrade.

## Observations about SpecScript

**Circular dependency in TypeScript.** `context.ts` and `run.ts` have a circular dependency for file resolution. The existing `setRunFileFn()` pattern (previously `setRunScriptFileFn()`) works but is a code smell. A future refactor could use a registry or dependency injection.

**Inline script context creation is fiddly.** In TypeScript, `DefaultContext` auto-sets built-in variables in its constructor, so creating a "clean" child context for inline scripts requires resetting variables after construction. This works but could be cleaner with a factory method or builder pattern.

## What to look for in the diff

- `Run.kt` is the new command — compare with the deleted `RunScript.kt` to see what was added (inline script parsing, `cd` resolution, `parentCommandLookup` wiring)
- `run.ts` is a full port, not a rename — the TypeScript version handles all the same cases
- `context.ts` changes are small but important: `parentCommandLookup` field and the `getCommandHandler` delegation
- The spec file moves from `plan/draft-specs/` into `specification/` — git tracked them as renames, so the diff should be readable
