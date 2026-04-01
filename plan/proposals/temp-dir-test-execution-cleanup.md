# Temp Dir and Test Execution Cleanup

## Problem Statement

SpecScript has two test execution paths for `.spec.md` files:

1. **JUnit `specificationTest`** (Kotlin `getCodeExamplesAsTests()`, TypeScript `spec-runner.test.ts`)
2. **`spec --test`** CLI command (Kotlin `runTests()`, TypeScript `runMarkdownTests()`)

Both paths need to handle the same core challenge: `temp-file=` blocks in Markdown create temp files (via the
`Temp file` command), and those files need to be findable by `Run script:`, `resource:`, and other commands that look
in `context.scriptDir`.

The current solution is a hack in the JUnit path: create a temp dir, set `scriptFile` to that temp dir (so
`scriptDir = tempDir`), then manually override `SCRIPT_HOME` back to the real spec file's directory. This works but
is confusing and creates inconsistency between the two test paths.

### Current state

| Aspect | JUnit path (both impls) | `spec --test` Kotlin | `spec --test` TypeScript |
|---|---|---|---|
| Temp dir created? | Yes, upfront | Yes (same code as JUnit) | No |
| `scriptDir` | Temp dir (the hack) | Temp dir (same hack) | Real file's directory |
| `SCRIPT_HOME` | Overridden to real dir | Overridden (same) | Real dir (natural) |
| `temp-file=` blocks findable? | Yes | Yes | No (broken) |

The Kotlin `spec --test` path calls `getCodeExamplesAsTests()` which uses the JUnit `DynamicTest` type, even though
it doesn't run under JUnit. This is a leaky abstraction — the CLI test runner shouldn't depend on JUnit types.

### Why `scriptDir = tempDir` is needed

The `temp-file=` mechanism converts file blocks to `Temp file` commands which write to `context.tempDir`. For those
files to be resolvable by commands that look in `context.scriptDir` (like `Run script:`, `resource:`), scriptDir must
equal tempDir. The hack achieves this by setting `scriptFile = tempDir`.

But then `SCRIPT_HOME` (initialized from `scriptDir`) would point to the wrong place, so it gets manually
overridden.

## Proposal A: Explicit Test Context

Introduce a dedicated concept: when running tests from Markdown, the context operates in "test mode" where the file
resolution path includes `tempDir` as a fallback.

### Changes

1. **Add `tempDir` as a fallback resolution path in `scriptDir`-based lookups.** Instead of making `scriptDir =
   tempDir`, teach the file resolution logic to check both `scriptDir` and `tempDir`. This means:
   - `resource:` resolution checks `scriptDir` first, then `tempDir`
   - `Run script:` / command lookup checks `scriptDir` first, then `tempDir`
   - `Temp file` still writes to `tempDir` (unchanged)

2. **Remove the `scriptFile = tempDir` hack from `getCodeExamplesAsTests()`.** Use the real spec file path as
   `scriptFile`. `SCRIPT_HOME` naturally points to the right place without manual override.

3. **Make `spec --test` for Markdown use the same logic** — just create a FileContext with the real spec file path.
   Temp dir is created lazily on first use (already how it works for `.spec.yaml`).

4. **Remove JUnit dependency from CLI test path.** Extract the core test discovery logic
   (`splitMarkdown` → filter sections → run) into a shared function that doesn't return `DynamicTest`. The JUnit
   path wraps results in `DynamicTest`; the CLI path iterates directly.

### Impact

- `SCRIPT_HOME` override hack: **deleted**
- `scriptFile = tempDir` hack: **deleted**
- `setTempDir()` pre-initialization: **deleted** (lazy creation suffices)
- 93 `temp-file=` blocks: **unchanged** (still write to tempDir)
- 58 `SCRIPT_TEMP_DIR` references: **unchanged** (tempDir still exists, same path)
- 14 `SCRIPT_HOME` references: **unchanged** (now naturally correct)
- TypeScript `spec --test`: **fixed** (currently broken for `temp-file=` blocks)

### Risk

Medium. The "check tempDir as fallback" logic touches file resolution which is used everywhere. Need to ensure
non-test contexts (normal `.spec.yaml` execution) aren't affected — they shouldn't be, since `tempDir` is only
populated when temp files are actually created.

One edge case: a spec creates a temp file with the same name as a real file in `scriptDir`. Today, the temp file
wins (because `scriptDir = tempDir`). With the fallback approach, the real file wins (scriptDir checked first).
This would be the correct behavior — real files should take precedence over temp files.

## ~~Proposal B: Minimal Fix~~ (Superseded)

Previously proposed porting the same hack to TypeScript. Now that the shell/CLI cleanup has established clean
defaults (`SCRIPT_HOME` as default working directory, `temp-file=` naming), this approach just perpetuates the
hack without benefit.

## ~~Proposal C: Make `Temp file` Write to `scriptDir` Instead~~ (Rejected)

Would pollute the user's project directory. The entire point of `tempDir` is disposable scratch space.

## Recommendation

**Proposal A** is the right approach. It removes three separate hacks, fixes the TypeScript `spec --test` path,
eliminates the JUnit type dependency from the CLI, and makes the mental model simpler: "scriptDir is where your
script lives, tempDir is scratch space, and file lookups check both."

The recent shell/CLI cleanup (SCRIPT_HOME defaults, `temp-file=` rename) makes Proposal A even more natural:
the naming now clearly communicates that `temp-file=` writes to a separate temp directory, so "check tempDir as
fallback" is an obvious resolution strategy.

## Implementation Plan for Proposal A

### Phase 1: Add tempDir fallback to file resolution

1. Identify all places that resolve files against `scriptDir`:
   - `resource:` resolution
   - `Run script:` / local command lookup in `FileContext`
   - Any other path resolution

2. Add `tempDir` as fallback (only when `tempDir` has been initialized — check `SCRIPT_TEMP_DIR` variable existence
   to avoid lazy-creating it unnecessarily).

3. Run specification tests to verify nothing breaks.

### Phase 2: Remove the hack from test setup

1. Change `getCodeExamplesAsTests()` to use real spec file path as `scriptFile`.
2. Remove `setTempDir()` pre-initialization.
3. Remove `SCRIPT_HOME` manual override.
4. Run specification tests.

### Phase 3: Extract shared test discovery from JUnit types

1. Create a function that returns test sections without wrapping in `DynamicTest`.
2. JUnit path wraps results in `DynamicTest`.
3. CLI path uses the unwrapped results directly.
4. TypeScript `runMarkdownTests` uses the same approach (no hack needed).

### Phase 4: Update TypeScript

1. Remove hack from `spec-runner.test.ts`.
2. Add fallback resolution to TypeScript context.
3. Ensure `spec --test` works for Markdown files.
