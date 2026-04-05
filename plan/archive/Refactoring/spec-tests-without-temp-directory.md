# Spec Tests Without Temp Directory

## Problem

Spec tests currently run in a temp directory. This means `${SCRIPT_TEMP_DIR}` appears in code examples throughout the
specification, which is confusing for first-time readers. The temp directory is an implementation detail of the test
infrastructure, not a language concept.

The goal: run spec tests in the spec file's own directory so code examples stay clean.

## Current state

TestUtil.kt has been changed to use `FileContext(file)` directly instead of creating a temp directory. This means
`scriptDir` now points to the real spec file location. The `tempDir` is still created lazily on demand when `temp-file`
blocks write files.

This breaks 18 tests. All follow the same pattern: a `temp-file=` code block creates a file (written to `tempDir`), then
a `yaml specscript` block references that file by bare name (resolved against `scriptDir`). Since `scriptDir` and
`tempDir` are no longer the same directory, the file isn't found.

## Attempted approach: cd= attribute (reverted)

Added a `cd=` attribute to `yaml specscript` code blocks, analogous to the existing `cd=` on `shell` and `cli` blocks.
The idea: spec authors write `cd=${SCRIPT_TEMP_DIR}` in the code block header (invisible in rendered Markdown), which
temporarily overrides `scriptDir` for that block's commands.

### Implementation details

- **FileContext.kt**: Made `scriptDir` overridable via a `scriptDirOverride` field and `withScriptDir(dir, block)`
  scoping function. The original lazy computation was preserved as `computedScriptDir`.

- **Script.kt**: Added a synthetic internal command `__with_script_dir__` that wraps a block's commands. In
  `toScript()`, when a `SpecScriptYaml` block has `cd=`, the parsed commands are wrapped in an ObjectNode with `cd` and
  `script` fields, emitted as a `Command("__with_script_dir__", wrapper)`. In `runCommands()`, this is intercepted
  before the normal command handler lookup.

- The `script` field was an `ArrayNode` (since `Yaml.parseAsFile()` returns `List<JsonNode>`). `Script.from(data)` on
  the ArrayNode correctly unpacks it via `toCommandList(JsonNode)` which handles arrays.

### Why it was reverted

The approach works technically but introduces hacks into the core execution loop:

1. A synthetic command (`__with_script_dir__`) in the command execution path that isn't a real command
2. `FileContext` gains mutable state (`scriptDirOverride`) that breaks the clean immutable-ish contract
3. The fix is per-block — all 18 spec files need individual annotation changes
4. It pushes test infrastructure concerns into the language runtime

## Alternative approaches to consider

### 1. Fix at the test infrastructure level

Instead of changing the language runtime, make `temp-file` blocks write to `scriptDir` (or a subdirectory of it) when
running in spec-test mode. The test runner already controls how `FileContext` is constructed — it could set `tempDir` to
a subdirectory of the spec file's directory (e.g., `.specscript-temp/`) and clean it up after the test.

Pros: No runtime changes, no spec file changes needed. Cons: Creates temp files alongside spec files (needs cleanup).

### 2. Make temp-file blocks resolve against tempDir explicitly

Change file resolution so that files created by `temp-file` blocks are tracked, and when a `Run script` or `Read file`
references a known temp-file name, it resolves against `tempDir` automatically.

Pros: Clean separation, invisible to spec authors. Cons: Implicit magic, could mask real resolution bugs.

### 3. Run tests in a temp directory but DON'T expose SCRIPT_TEMP_DIR in examples

Keep the current temp-directory approach but refactor the 18 failing specs so they don't use `${SCRIPT_TEMP_DIR}` in
their visible code. Instead, use `file=` attributes or restructure examples to avoid referencing temp files by path.

Pros: No runtime changes needed. Cons: May require significant spec rewrites; some examples legitimately need file
paths.

### 4. Set tempDir = scriptDir in test mode only

In `TestUtil.getCodeExamplesAsTests()`, after creating `FileContext(file)`, call `context.setTempDir(scriptDir)`. This
makes temp files land in the spec file's directory. Add cleanup in an `@AfterAll` or similar.

Pros: One-line fix in test infrastructure. No runtime changes. No spec changes. Cons: Temp files created alongside spec
files during test runs. Must ensure cleanup. Risk of accidentally committing generated files.

## Failing tests (18)

All follow the pattern: `temp-file=` creates a `.spec.yaml` file, then `yaml specscript` references it by bare name.

| Spec file                                            | Test section                           |
|------------------------------------------------------|----------------------------------------|
| `Variables.spec.md`                                  | Script output                          |
| `Variables.spec.md`                                  | Script Input & Output                  |
| `Testing.spec.md`                                    | Testing                                |
| `Organizing SpecScript files in directories.spec.md` | Calling another script                 |
| `Input parameters.spec.md`                           | Using types                            |
| `Validate schema.spec.md`                            | Schema from file                       |
| `Create credentials.spec.md`                         | Basic usage                            |
| `Connect to.spec.md`                                 | Connection inheritance                 |
| `Http server.spec.md`                                | Running a script file                  |
| `SpecScript files as commands.spec.md`               | Basic usage                            |
| `Read file.spec.md`                                  | Reading temp files created in Markdown |
| `Run script.spec.md`                                 | Basic usage                            |
| `Run script.spec.md`                                 | Passing input parameters               |
| `Run script.spec.md`                                 | Finding the script                     |
| `Temp file.spec.md`                                  | Resolve variables                      |
| `Mcp server.spec.md`                                 | Deriving metadata from script          |
| `Mcp server.spec.md`                                 | Tools as a list of scripts             |
| `Mcp tool.spec.md`                                   | Deriving metadata from script          |
