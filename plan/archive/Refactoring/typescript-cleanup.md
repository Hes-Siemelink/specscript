# TypeScript Cleanup: Remove Level-Era Leftovers and Hidden Complexity

**Status**: Completed (Phases 1-5b/5c done: specscript-ac5d, specscript-2x0q, specscript-gvmw, specscript-zd7p)  
**Date**: 2026-07-14  
**Context**: Code review after porting test runner to auto-discovery

## Problem

The TypeScript implementation has accumulated leftover complexity from the "Level" system (Level 0, Level 3, etc.) that was used during incremental porting. The test runner now auto-discovers specs and registers all commands unconditionally, making level references misleading. Additionally, there are several workarounds and architectural quirks that add friction.

## Findings from Code Review

### High Priority

#### 1. Level References Everywhere
- `src/commands/register.ts`:
  - 7 section comments `// --- Level N ---` are stale
  - JSDoc says "Register all commands (Level 0 + Level 1 + Level 3 + Level 4)" — misleading
  - `registerLevel0Commands()` at line 192 is a `@deprecated` wrapper with no callers
- `src/language/context.ts:396`:
  - Error message: `"Register Level 3 commands before using local file commands"`
  - The whole `setRunFileFn` indirection (line 381) exists only for circular dep workaround
  - Since Run is always registered, this error branch is unreachable
- `src/markdown/converter.ts:21-31`:
  - JSDoc classifies block types by Level
  - Misleading now that TS registers all commands
- `src/language/script.ts:17-23`:
  - `skippedBlocks: string[]` field on Script
  - Wired through `blocksToScript` → test runner (`spec-runner.test.ts:210`)
  - Always empty in practice since ShellBlock/ShellCli/YamlFile all convert to commands
  - The `it.skip(\`${title} (needs ${script.skippedBlocks[0]})\`)` branch is dead code

#### 2. Legacy Test Format Branches
- `cli.ts:793-852` (`runYamlTests`):
  - Separate code paths for `Tests:` (new) vs `Test case:` (legacy)
  - Uses `hasNewTests` / `hasLegacyTests` flags
- `spec-runner.test.ts:99-179`:
  - Duplicated logic in `runStructuredTests` vs `runFlatTests`
- `src/language/script.ts`:
  - Two methods: `splitTests()` vs `splitTestCases()`
  - If both formats are still valid, unify via one entry point
  - If only one is canonical, delete the legacy branch

### Medium Priority

#### 3. Dual Markdown Execution Paths
`splitMarkdownSections` is called in 4 places, each rebuilding the same loop:
- `cli.ts:720` (executeFile)
- `cli.ts:861` (runMarkdownTests)
- `commands/run.ts:121` (runMarkdownScript)
- `spec-runner.test.ts:184` (runMarkdownFile)

Each repeats: "for each section, reset session state, run script"

`executeFile` in cli.ts (line 715+) duplicates logic already in `runMarkdownScript` (run.ts:119).

**Fix**: Push into a helper on Script or a new MarkdownFile class.

#### 4. Fragile Node API Cast in Test Runner
`spec-runner.test.ts:80`:
```typescript
const abs = join(e.parentPath ?? (e as unknown as {path: string}).path, e.name)
```
Fallback `(e as unknown as {path: string})` is for old Node versions. `package.json` requires `>=20`, and `parentPath` has been stable since Node 20.12.

**Fix**: Drop the cast, use `e.parentPath` directly.

#### 5. `runInlineScript` Reinvents Context Wheel
`commands/run.ts:134-163`:
- Manually sets `SCRIPT_HOME`, `PWD`, `env`, `input` variables
- Blanks out `variables` after construction
- All that logic already exists in `DefaultContext` constructor
- Manual `parentCommandLookup` wiring at line 157 only exists because inline scripts want parent's local file commands but not parent variables

**Fix**: Add `inheritCommands: boolean` option or factory method `parent.createInlineChildContext(cd)` rather than construct + mutate.

### Low Priority

#### 6. Unused Imports
- `command-handler.ts:1` — imports `JsonObject`, never used
- `command-execution.ts:2` — imports `JsonObject` and `isObject`, never used

#### 7. Hand-Rolled Type Conversion Helper
`util/yaml.ts:118-121` — `scalarToString` calls `String(node)` on non-scalars. The `yaml` package's `Pair.key` is always a Scalar in practice; else branch is dead. Same for `nodeToJson` fallback at line 155 (unreachable given type of node).

#### 8. `stripBlockScalarNewlines` Complexity Leak
`util/yaml.ts:12-16` + `converter.ts:56`:
- Flag exists because JS yaml lib always appends `\n` to block scalars
- Should be hidden inside yaml util's Markdown-facing entry point
- Only one caller (markdown converter) passes `true`

**Fix**: Make `parseYamlCommands` default to `true` when called from markdown converter, hide parameter.

#### 9. `deleteOnShutdown` Singleton in context.ts
`context.ts:403-420`:
- Module-global `pendingCleanupDirs` + `cleanupRegistered` flag
- `process.on('exit', ...)` for temp dirs
- Two issues:
  - `process.on('exit')` doesn't fire on `SIGINT`/uncaught rejection
  - This belongs in `util/`, not language core
- Kotlin uses `File.deleteOnExit` which has same limitation, so parity is fine
- But module split is wrong

#### 10. Sample Server Special-Cased in Test Runner
`spec-runner.test.ts:38, 73, 236-241`:
- `SAMPLE_SERVER` is hardcoded relative path
- Excluded from discovery
- Manually run in `beforeAll`

**Resolved**: Keep as-is. All tests can assume the sample server is running; no convention-based setup needed. Not
part of the cleanup.

## Proposed Plan

### Phase 1: Low-Risk Cleanup (Single Commit)
1. Remove `skippedBlocks` field from Script class
2. Remove `skippedBlocks` check in spec-runner.test.ts:210-214
3. Remove Level comments in `converter.ts:21-31` and `converter.ts:81`
4. Remove Level section comments in `register.ts` (keep flat list)
5. Delete `registerLevel0Commands()` at register.ts:192
6. Update `registerAllCommands()` JSDoc to remove Level references
7. Delete unused `JsonObject` import from `command-handler.ts`
8. Delete unused `JsonObject` and `isObject` imports from `command-execution.ts`
9. Drop `parentPath` fallback cast in `spec-runner.test.ts:80`
10. Rewrite error message at `context.ts:396` to remove Level reference

**Estimated effort**: 30 minutes  
**Risk**: Very low (all dead code / comments)

### Phase 2: Test Format Unification (Separate Commit)
Decision needed: Are both `Tests:` and `Test case:` still valid SpecScript syntax, or is one legacy?

**Option A**: Both are valid (keep both)
- Unify `cli.ts:runYamlTests` and `spec-runner.test.ts` logic
- Make both call `Script.splitTests()` or `Script.splitTestCases()` based on detection
- Extract shared test-running logic to helper function

**Option B**: Only `Tests:` is canonical (drop legacy)
- Delete `Script.splitTestCases()` method
- Delete `Test case:` branches in cli.ts and spec-runner.test.ts
- Check specification/ for any lingering `Test case:` usage

**Resolved**: Option A. `specification/commands/core/testing/Test case.spec.md` documents it as the legacy command
but it is still executable spec — both formats stay valid. Unify `cli.ts:runYamlTests` and `spec-runner.test.ts`
logic around one shared entry point rather than deleting either branch.

**Estimated effort**: 1-2 hours  
**Risk**: Medium (changes test execution model)

### Phase 3: Markdown Execution Consolidation (Separate Commit)
Extract "run markdown sections with shared context" into single helper:

```typescript
// In src/language/markdown-runner.ts or src/cli-util.ts
export async function runMarkdownFile(
  content: string,
  context: ScriptContext,
  resetBetweenSections: boolean = true
): Promise<JsonValue | undefined>
```

Update 4 call sites:
- `cli.ts:715-730` (executeFile)
- `cli.ts:854-879` (runMarkdownTests)
- `commands/run.ts:119-129` (runMarkdownScript)
- `spec-runner.test.ts:181-224` (runMarkdownFile)

**Estimated effort**: 1 hour  
**Risk**: Medium (touches execution paths)

### Phase 4: Inline Script Context Factory (Separate Commit)
Replace `runInlineScript` manual variable setup with factory method:

```typescript
// In DefaultContext class
createInlineChildContext(workingDir?: string): DefaultContext {
  const child = new DefaultContext({
    scriptFile: workingDir ?? this.scriptDir,
    interactive: this.interactive,
    session: this.session,
    workingDir: workingDir ?? this.workingDir,
  })
  child.parentCommandLookup = this
  // Fresh variables — no parent variables leak in
  child.variables = new Map()
  child.variables.set('input', {})
  return child
}
```

Then `runInlineScript` becomes 5 lines.

**Estimated effort**: 30 minutes  
**Risk**: Low (refactoring)

### Phase 5: Architectural Improvements (Optional)
#### 5a: `setRunFileFn` Circular Dep Dance
**Resolved**: No clear win either way. Leave as-is, but replace the vague error message (already fixed in Phase 1)
and add a one-line comment at the `setRunFileFn` declaration explaining it exists to break the `context.ts` /
`run.ts` circular dependency. Revisit with a module split only if the circular-dep workaround spreads beyond this
one function.

**Estimated effort**: 5 minutes (comment only)  
**Risk**: None

#### 5b: Hide `stripBlockScalarNewlines` Parameter
Make `parseYamlCommands` automatically strip when called from markdown converter:

```typescript
// Add second entry point
export function parseMarkdownYamlCommands(content: string): Command[] {
  return parseYamlCommands(content, true)
}
```

Update converter.ts to use new entry point.

**Estimated effort**: 15 minutes  
**Risk**: Very low

#### 5c: Move Temp Dir Cleanup to util/
Extract `deleteOnShutdown` logic from `context.ts` into `util/temp-dir.ts`:

```typescript
// util/temp-dir.ts
export function createTempDir(): string {
  const dir = mkdtempSync(join(tmpdir(), 'specscript-'))
  registerCleanup(dir)
  return dir
}
```

**Estimated effort**: 30 minutes  
**Risk**: Low

## Recommendation for Next Session

**Start with Phase 1** (Low-Risk Cleanup) as a single commit. It removes stale comments, dead code, and misleading references with zero behavioral change.

**Before Phase 2**, check specification/ directory:
```bash
grep -r "Test case:" specification/
```
If zero hits, delete the legacy branch. If there are hits, unify the two code paths.

**Phase 3-5 are optional refactors** — only do if the codebase will be actively developed. If TS port is "done" and maintenance-only, skip.

## Open Questions

1. **Test format**: Is `Test case:` still valid syntax, or legacy?
2. **Sample server**: Should test setup be convention-based (e.g., `test/setup.spec.yaml`) or keep hardcoded?
3. **Circular dep**: Is module split worth it, or just document the pattern?

## Files Affected (Phase 1 Only)

- `src/commands/register.ts` — remove Level comments, delete deprecated function
- `src/language/context.ts` — rewrite error message at line 396
- `src/language/script.ts` — remove `skippedBlocks` field
- `src/language/command-handler.ts` — remove unused import
- `src/language/command-execution.ts` — remove unused imports
- `src/markdown/converter.ts` — remove Level comments
- `test/spec-runner.test.ts` — remove skippedBlocks check, drop parentPath cast

**Total**: 7 files, ~20 lines deleted, ~5 lines changed.
