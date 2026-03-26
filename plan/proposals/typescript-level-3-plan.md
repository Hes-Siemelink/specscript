# TypeScript Level 3 Implementation Plan: Files, Shell, Script Composition

## Scope

Level 3 turns SpecScript from a pure data-manipulation language into an automation tool. Six new
commands interact with the OS: file I/O (`Read file`, `Write file`, `Temp file`), process spawning
(`Shell`), script composition (`Run script`, local file commands), and CLI self-invocation (`Cli`).

Three Markdown block types become executable: `yaml file=`, `shell`, `shell cli`.

## Implementation Order

Commands are ordered by dependency — each command only depends on commands implemented before it.

### 1. Context updates (SCRIPT_HOME, SCRIPT_TEMP_DIR, env, workingDir, scriptDir, tempDir)

The existing `DefaultContext` already sets `SCRIPT_HOME` and `env`. New additions:
- Add `workingDir` property (defaults to `process.cwd()`)
- Add `scriptDir` property (derived from `scriptFile`)
- Add `tempDir` property (lazy creation, stored in variables as `SCRIPT_TEMP_DIR`)
- Add `scriptFile` as an absolute path (currently not normalized)
- Ensure child contexts share `session` but get fresh `variables`

These properties are needed by every Level 3 command.

### 2. Temp file (DelayedResolver)

- Creates temp files in `context.tempDir`
- Value form: content as string, creates random-named file
- Object form: `{ filename?, resolve? (default true), content }`
- When `resolve: false`, variables in content are NOT resolved
- Returns absolute path as string
- Files registered for cleanup (best-effort, using process exit hook)
- Implements `delayedResolver: true` on the handler

No dependencies on other Level 3 commands.

### 3. Read file

- Value form: resolves filename against `workingDir`
- Object form: `file:` resolves against `workingDir`, `resource:` resolves against `scriptDir`
- Returns parsed YAML content (multi-document returns array)
- Throws `CommandFormatError` if file not found

Depends on: context `workingDir`/`scriptDir`.

### 4. Write file

- Value form: filename string, writes `context.output`
- Object form: `{ file, content? }`, falls back to `context.output`
- Creates parent directories automatically
- Serializes via `toDisplayYaml()`
- Path used directly (no workingDir resolution — matches Kotlin behavior)

Depends on: Read file (for verification in tests).

### 5. Shell

- Value form: command string, runs in `workingDir`
- Object form: `{ command?, resource?, cd?, show output?, show command?, capture output?, env? }`
- Always runs via child_process with `/bin/bash -c command`
- Merges stderr into stdout
- Exposes all script variables as env vars (serialized to YAML strings)
- Sets `SCRIPT_HOME` env var
- Working directory: `cd` overrides; `workingDir` for `command`; `scriptDir` for `resource`
- Non-zero exit throws `SpecScriptCommandError` with type `"shell"` and `exitCode` data
- Resets `ExpectedConsoleOutput` before execution
- `captureOutput` default true, `showOutput` default false

Depends on: context `workingDir`/`scriptDir`, `SCRIPT_HOME`.

### 6. Run script + local file commands + specscript-config.yaml

- **Run script** value form: resolves against `scriptDir` (unlike Read file which uses workingDir)
- **Run script** object form: `{ file?, resource?, input? }`
- Creates child context with fresh variables (`input`, `SCRIPT_HOME`, `env`) but shared `session`
- Parses and runs the target file (supports both .spec.yaml and .spec.md)
- Auto-list iteration on `input` array

- **Local file commands**: When a command name isn't found in the registry, scan `scriptDir` for
  matching `.spec.yaml`/.spec.md` files. Convert filename to command name: `create-greeting.spec.yaml`
  → `Create greeting`. The file is loaded, parsed, and executed as a command with `input` bound
  to the command data.

- **specscript-config.yaml imports**: Load `imports` list from config, resolve relative to `scriptDir`,
  make those files available as commands.

Depends on: Read file (for YAML parsing), context child creation.

### 7. Cli

- Value form: command string (e.g., `--help`, `spec basic`)
- Object form: `{ command, cd? }`
- Default working dir is `context.tempDir`
- Splits command on whitespace, drops first token if it's "spec"
- **In-process invocation**: Import and call a `runCli(args, workingDir)` function exported
  from `cli.ts`. This avoids spawning a subprocess.
- Captures stdout/stderr, stores via `ExpectedConsoleOutput.storeOutput()`

This is the most complex command and depends on everything else being working.

### 8. Update Markdown converter

Three block types currently skipped become executable:
- `yaml file=filename` → `Temp file` command with the block content
- `shell` → `Shell` command with the block content
- `shell cli` → `Cli` command with the block content

The converter needs to:
- Parse `file=filename` from the header line for YamlFile blocks
- Handle `cd=dir` and `ignore` modifiers on shell blocks
- Generate the appropriate commands

### 9. Test file wiring

Add to `spec-runner.test.ts`:
- `LEVEL_3_TEST_FILES` array with all .spec.yaml test files
- `LEVEL_3_MD_FILES` array with all .spec.md test files
- Un-skip 3 previously-skipped tests
- Update skip list for tests that still depend on higher levels

## Test Expectations

### spec.yaml tests (25 tests across 5 files)
- Read file tests: 3 tests
- Run script tests: 7 tests
- Save as tests: 1 test
- Locate files in the same way: 11 tests
- Shell tests: 3 tests

### spec.md tests (~56 sections across spec files)
- Read file.spec.md: 4 sections
- Write file.spec.md: 2 sections
- Temp file.spec.md: 4 sections
- Run script.spec.md: 4 sections
- SpecScript files as commands.spec.md: 1 section
- Shell.spec.md: ~12 sections
- Cli.spec.md: 2 sections

### Previously-skipped tests to un-skip (3)
- `SCRIPT_HOME is different from SCRIPT_TEMP_DIR` — needs Temp file
- `For each with variable syntax in sample data` — needs Read file
- `For each with variable syntax in sample data and implicit loop variable` — needs Read file

## Files to create
- `typescript/src/commands/files.ts` — Read file, Write file, Temp file commands
- `typescript/src/commands/shell.ts` — Shell command
- `typescript/src/commands/cli-command.ts` — Cli command (separate from cli.ts entry point)
- `typescript/src/commands/run-script.ts` — Run script, local file command resolution

## Files to modify
- `typescript/src/language/context.ts` — Add workingDir, scriptDir, tempDir, file command resolution
- `typescript/src/commands/register.ts` — Register Level 3 commands
- `typescript/src/markdown/converter.ts` — Handle yaml file=, shell, shell cli blocks
- `typescript/src/cli.ts` — Export runCli() for in-process Cli command
- `typescript/test/spec-runner.test.ts` — Level 3 test files and un-skips

## Design decision: Cli command implementation

Kotlin runs `SpecScriptCli.main(args)` in-process. For TypeScript, we'll do the same by extracting
the core logic from `cli.ts` into a callable `runCli(args, workingDir)` function. The Cli command
imports and calls this function, capturing stdout via the existing stdout capture mechanism.

## Estimated scope
- ~600-800 lines of new TypeScript code
- ~50 lines of modified code across existing files
- Target: ~84 new tests passing
