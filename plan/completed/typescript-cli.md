# TypeScript CLI Implementation

## Problem

The TS CLI (`typescript/src/cli.ts`) is a stub that only accepts a bare filename and runs it. It lacks global options,
directory invocation, command arguments, output printing, error reporting, and a runnable binary.

**Out of scope:** `--interactive` mode (user prompts for command selection).

**Success criteria:** The existing vitest suite (226 tests) continues to pass, including the `Cli` command tests in
`Cli.spec.md` which invoke the CLI in-process and validate exact output.

## Implementation Steps

### Step 1: Bundling and `spec-ts` binary

Do this first so we can test interactively throughout all subsequent steps.

**tsup** bundles into a single file. Config: entry `src/cli.ts`, ESM format, Node 22 target, bundle the `yaml`
dependency, add `#!/usr/bin/env node` banner.

**`spec-ts` wrapper** at `~/.local/bin/spec-ts` (same pattern as the Kotlin `spec` wrapper):
```bash
#!/bin/bash
exec node /path/to/specscript/typescript/dist/cli.js "$@"
```

npm scripts: `build` → tsup, `build:dev` → tsc (source maps), `link:cli` → install wrapper.

### Step 2: Option parsing and core CLI

Argument parsing mirroring Kotlin's `CliCommandLineOptions` / `splitArguments()`:

```
spec-ts [global options] file|directory [command options]
```

State machine: leading `--flags` → global options, non-flag tokens → commands (file path + optional subcommand),
remaining `--flags` and values → command parameters.

Global options defined in `specscript-command-line-options.yaml`: `--help`/`-h`, `--output`/`-o`,
`--output-json`/`-j`, `--debug`/`-d`, `--test`/`-t`, `--interactive`/`-i` (accepted but ignored).

Output defaults to YAML — the spec states "By default, the cli command prints the output in Yaml format"
([Command line options.spec.md](../../specification/cli/Command%20line%20options.spec.md)).

Exit codes: 0 success, 1 error. (Kotlin always exits 0 due to a bug — fix this in TS.)

Spec references:
- [Cli.spec.md](../../specification/commands/core/shell/Cli.spec.md) — usage banner exact match
- [Running SpecScript files.spec.md](../../specification/cli/Running%20SpecScript%20files.spec.md) — no args, single
  file, extension omission

### Step 3: Script help and parameters

`--help` on a file prints `Script info` description + `Input schema`/`Input parameters` properties:

```
Description text

Options:
  --name   Your name
```

Command parameters: `--name Alice` after the filename → `{ name: "Alice" }` in input variables. Uses Kotlin's
`toParameterMap()` logic: flags become keys, next non-flag token is the value.

Spec references:
- [Command line options.spec.md](../../specification/cli/Command%20line%20options.spec.md) — `--help` section
- [Running SpecScript files.spec.md](../../specification/cli/Running%20SpecScript%20files.spec.md) — "Supplying input"
- [Input schema.spec.md](../../specification/commands/core/script-info/Input%20schema.spec.md) — "Cli help"
- [SpecScript Yaml Scripts.spec.md](../../specification/language/SpecScript%20Yaml%20Scripts.spec.md) — "Script info",
  "Defining script input"

### Step 4: Directory invocation

When the resolved path is a directory:

1. Load description from `specscript-config.yaml` (`Script info` field) or fall back to `README.md` first paragraph
2. List `.spec.yaml`/`.spec.md` files and subdirectories as commands
3. If `--help` or no subcommand: print description + command list
4. If subcommand given: resolve and execute recursively (`spec-ts samples basic greet`)

Hidden scripts (`Script info: { hidden: true }`) are excluded from listings.

This logic already partially exists in `cli-command.ts`'s `printDirectoryInfo()` — move to shared code.

Spec references:
- [Cli.spec.md](../../specification/commands/core/shell/Cli.spec.md) — directory listing exact match
- [Script info.spec.md](../../specification/commands/core/script-info/Script%20info.spec.md) — "Hidden commands"
- [Running SpecScript files.spec.md](../../specification/cli/Running%20SpecScript%20files.spec.md) — "Running a
  directory", command chaining

### Step 5: Error reporting

Match Kotlin's `CliErrorReporter` formatting:

- **CLI invocation errors:** just the message ("Invalid option: --foo", "Could not find spec file for: X")
- **Script errors:** "Scripting error" header, message, optional YAML data, command context
- **Command errors:** message + optional details
- **Missing parameters:** "Missing parameter: --name" + options list
- **`--debug`:** full stack traces (Node.js format)

Spec references:
- [Command line options.spec.md](../../specification/cli/Command%20line%20options.spec.md) — "--debug" section

### Step 6: Consolidate cli-command.ts

Refactor `cli-command.ts` to call shared `runCli()` with captured stdout/stderr instead of its own parallel
implementation. Remove duplicated `printUsage()`, `printDirectoryInfo()`, `extractDescriptionFromMarkdown()`, etc.

### Step 7: Test mode

`--test` runs tests and prints a YAML report:
```yaml
details: []
failed: 0
passed: 3
```

Requires:
- `Script.splitTestCases()` / `Script.splitTests()` to split YAML scripts at `Tests`/`Test case` boundaries
- Markdown: extract `Code example` sections as tests (reuse `splitMarkdownSections()`)
- Directory: recursive test discovery
- Files without tests: report `passed: 0, failed: 0`

This is the most complex step. Kotlin reference: `TestUtil.kt` `runTests()` / `getTestCases()`.

## Current State

- `cli.ts` (114 lines): bare `main()`, `runCli()`, `resolveCommand()`, `executeFile()`
- `cli-command.ts` (253 lines): `Cli` command with duplicated CLI logic (usage, directory listing, arg parsing)
- No bundler config. Build is plain `tsc` to `dist/`.

## Verification

After each step, compare `spec` vs `spec-ts` on the same inputs:

```bash
spec-ts samples/basic/greet.spec.yaml          # run file (step 2)
spec-ts --help samples/basic/greet.spec.yaml   # script help (step 3)
spec-ts samples/basic greet --name Alice       # dir + params (step 3-4)
spec-ts -j samples/basic/output.spec.yaml      # JSON output (step 2)
spec-ts samples/basic                          # directory listing (step 4)
spec-ts -t specification/commands/core/testing/tests/Assert\ tests.spec.yaml  # test mode (step 7)
```

Run `npx vitest run` after each step — existing 226 tests must stay green.

Edge cases tracked in [typescript-cli-edge-cases.md](typescript-cli-edge-cases.md).
