# TypeScript: Package System Implementation Plan

Bring the TypeScript implementation up to date with the Kotlin package system.

## Current State

The TypeScript version has a basic import system in `DefaultContext.findImportedCommand()` (context.ts:224-245) that
only supports the **old array format**:

```yaml
imports:
  - helper/say-something.spec.yaml
```

It reads `config.imports` as an array of file path strings and resolves them one at a time. No package support, no
wildcard imports, no aliases, no local `./` prefix.

The Kotlin implementation now has the full package system: `PackageImport.parse()`, `ImportItem` sealed hierarchy,
`PackageRegistry`, `--package-path` CLI flag, wildcard/alias/local imports.

## Breakage from Kotlin Changes

8 TypeScript spec tests are currently failing due to Kotlin-side changes:

- **5 help output tests** — CLI help alignment changed when `--package-lib-path` was renamed to `--package-path`
  (shorter name = different column padding). These are in `Cli.spec.md`, `Command line options.spec.md`,
  `Running SpecScript files.spec.md`, `SpecScript Markdown Documents.spec.md`.
- **3 import tests** — The spec now uses the new map-based import format which the TypeScript `findImportedCommand()`
  doesn't understand: `Organizing SpecScript files in directories.spec.md` and two `Run script tests.spec.yaml` cases.

## Implementation Plan

### Phase 1: Fix the 5 help output failures

The TypeScript CLI produces its own help output (in `cli.ts`). Update it to match the new Kotlin help output — add the
`--package-path, -p` option and fix alignment.

Alternatively, since the TypeScript `shell cli` blocks invoke the TypeScript CLI in-process, the help output is generated
by TypeScript code. The spec tests compare against the expected output in the spec file (which we already updated). So
the TypeScript CLI help formatter needs to produce identical output.

### Phase 2: Implement import parsing

Port the import data model and parser. In TypeScript this maps naturally to:

```typescript
// PackageImport type + ImportItem discriminated union
interface PackageImport {
  source: string
  items: ImportItem[]
  local: boolean
}

type ImportItem =
  | { kind: 'command'; path: string; name: string }
  | { kind: 'name'; value: string }
  | { kind: 'directory'; path: string; alias?: string }
  | { kind: 'wildcard'; path: string; recursive: boolean }
```

The `PackageImport.parse()` companion method becomes a standalone `parseImports(imports: JsonValue): PackageImport[]`
function. The private helpers (`parseImportItems`, `parseElement`, `parseImportString`) stay as module-private functions.

Reject array-format imports with an error (matching Kotlin behavior).

### Phase 3: Implement PackageRegistry

Port `PackageRegistry` as a module with:

- `packagePath: string | undefined` — set by CLI
- `findPackage(name: string): string | undefined` — search path lookup
- `scanCommands(packageDir, items): Map<string, CommandHandler>` — resolve import items to handlers
- `scanLocalCommands(configDir, localPath, items): Map<string, CommandHandler>` — resolve local imports

Search path order (matching Kotlin):
1. `--package-path` / `-p` CLI argument
2. `SPECSCRIPT_PACKAGE_PATH` env var (colon-separated)
3. `~/.specscript/packages/`

The TypeScript version uses `readdirSync` / `statSync` for file operations (no async needed here — import resolution
happens once at context creation time).

### Phase 4: Integrate into DefaultContext

Replace `findImportedCommand()` in `DefaultContext` with import resolution that uses `PackageRegistry`:

1. Read `specscript-config.yaml`, parse `imports` via `parseImports()`
2. For each `PackageImport`:
   - If `local`: call `PackageRegistry.scanLocalCommands()`
   - Else: call `PackageRegistry.findPackage()` then `PackageRegistry.scanCommands()`
3. Cache the result (lazy, like Kotlin's `by lazy`)

Also read `Package info` from config to support package declarations.

### Phase 5: CLI flag

Add `--package-path` / `-p` to the TypeScript CLI argument parser in `cli.ts`. Set
`PackageRegistry.packagePath` before dispatching.

### Phase 6: Update SKIP_TESTS

Remove the now-passing tests from `SKIP_TESTS` in `spec-runner.test.ts`:
- `Importing commands from another directory`
- `Imported helper scripts`
- The two `Run script tests` import cases

Add the Packages spec tests to the test file lists (either Level 3 or a new Level 6).

### Phase 7: Run full test suite

Verify all previously-passing tests still pass and the new import/package tests pass.

## Files to Create/Modify

| File | Action |
|------|--------|
| `typescript/src/language/package-import.ts` | **New** — `PackageImport`, `ImportItem`, `parseImports()` |
| `typescript/src/language/package-registry.ts` | **New** — Package discovery, command scanning |
| `typescript/src/language/context.ts` | **Modify** — Replace `findImportedCommand()`, read `Package info` |
| `typescript/src/cli.ts` | **Modify** — Add `--package-path`/`-p` flag, update help output |
| `typescript/test/spec-runner.test.ts` | **Modify** — Add Packages spec tests, remove skips |

## Estimated Effort

This is a straightforward port — the Kotlin implementation is the reference. No design decisions needed, just
translation. The import parser is ~85 lines in Kotlin; the registry is ~200 lines. The TypeScript equivalents will be
similar in size.

Half a day of agent work, including test validation.
