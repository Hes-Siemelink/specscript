# Level 3 Implementation Report

Findings from the TypeScript Level 3 implementation (179/188 tests passing, 9 expected skips).

## Test Results

| Suite | Passed | Skipped | Failed |
|---|---|---|---|
| Level 0 | 37 | 1 | 0 |
| Level 1 | 64 | 3 | 0 |
| Level 2 | 33 | 0 | 0 |
| Level 3 | 45 | 5 | 0 |
| **Total** | **179** | **9** | **0** |

Remaining skips are Level 4+ dependencies (HTTP client, MCP server, Validate schema, Prompt).

## Scope

Level 3 turns SpecScript from a data-manipulation language into an automation tool. Six commands
interact with the OS: file I/O (Temp file, Read file, Write file), process spawning (Shell),
script composition (Run script, local file commands), and CLI self-invocation (Cli).

Three Markdown block types become executable: `yaml file=`, `shell`, `shell cli`.

## 1. YAML block scalar trailing newline: JS yaml vs Jackson

**The most subtle bug in the TypeScript implementation.** The JS `yaml` library and Jackson handle
YAML block scalars (`|` and `>`) differently when the source string doesn't end with a newline.

The YAML spec says block scalars include a trailing newline. Both libraries comply — but Jackson
has an undocumented shortcut: when the entire YAML source string has no trailing newline, it strips
the trailing `\n` from block scalar values. The JS `yaml` library always includes it.

This matters because `getContent()` in the Markdown scanner joins lines with `\n` but produces
no trailing newline. So when a spec.md code block contains:

```yaml
Temp file: |
  My content
```

...the YAML source has no trailing newline. Jackson produces `"My content"`, JS yaml produces
`"My content\n"`.

The failure chain:
1. `Temp file: |` → JS yaml parses → `"My content\n"` → written to file with trailing newline
2. `Read file` → reads file → parses as YAML → `"My content"` (plain scalar, no trailing `\n`)
3. `Expected output: |` → JS yaml parses → `"My content\n"`
4. Comparison: `"My content"` ≠ `"My content\n"` → **FAIL**

**Fix:** `parseYamlCommands` accepts a `stripBlockScalarNewlines` flag. When true, `nodeToJson`
detects `BLOCK_LITERAL`/`BLOCK_FOLDED` scalar AST node types and strips the trailing `\n`. The
Markdown converter passes `true` (since `getContent()` never ends with `\n`); `.spec.yaml` file
callers use the default `false` (since files read from disk do end with `\n`, matching Jackson).

**For the Go implementer:** `gopkg.in/yaml.v3` may exhibit similar behavior. Test block scalar
values parsed from strings without trailing newlines. If Go's library always adds `\n`, apply
the same conditional stripping based on whether the source ends with a newline.

**For the SpecScript maintainer:** This is a latent spec portability issue. The spec tests pass
in Kotlin only because `getContent()` happens to lack a trailing newline and Jackson happens to
strip block scalar newlines in that case. If either behavior changed, spec.md tests involving
block scalars would break. Consider making the spec explicit about whether block scalar content
should include the trailing newline.

## 2. ExpectedConsoleOutput trims; ExpectedOutput does not

Kotlin's `ExpectedConsoleOutput` trims both sides before comparing:
`output.trim() != data.toDisplayYaml().trim()`. But `ExpectedOutput` uses strict equality:
`output != data` (Jackson's structural equals).

The TypeScript implementation initially used strict comparison for both. This caused failures
because the console capture mechanism and YAML serialization produce slightly different
whitespace. Adding `.trim()` to `ExpectedConsoleOutput` fixed the issue.

**For the Go implementer:** Use trimmed comparison for `Expected console output` but strict
structural equality for `Expected output`. These are different commands with different semantics.

## 3. Cli command: in-process execution complexity

The `Cli` command is the most complex Level 3 command. Kotlin calls `SpecScriptCli.main(args)`
in-process, which reuses the entire CLI stack. The TypeScript implementation can't do the same
because the CLI entry point has side effects (process.exit calls, stdin handling).

Instead, `runCliInProcess` reimplements the core CLI logic: flag parsing (`--help`/`-h`),
command resolution (exact match → `.spec.yaml` → `.spec.md`), directory listing with README
description extraction and Script info parsing.

This duplication is fragile — CLI behavior changes must be mirrored in two places. But it's
the pragmatic choice for a test-oriented implementation.

**For the Go implementer:** If your CLI entry point is structured as a library function that
returns results rather than calling `os.Exit()`, the Cli command can simply call it. Design
the CLI with in-process invocation in mind from the start.

**For the SpecScript maintainer:** The Cli command implicitly depends on the exact format of
`--help` output, directory listing format, and README description extraction. These are all
tested through `Expected console output` assertions in spec files. Any formatting change to
the CLI breaks these tests. Consider whether the Cli command tests should use more flexible
assertions (e.g., `contains:` instead of exact string match).

## 4. Spec.md test runner context: scriptDir vs tempDir

When running spec.md tests, the test runner must set up a context where:
- `scriptDir` and `tempDir` point to a fresh temp directory (so `file=` blocks create files there)
- `SCRIPT_HOME` points to the real spec file's parent (so `resource:` lookups find sibling files)

This is because spec.md files create temporary files via `yaml file=` blocks that need to exist
in the working directory, but also reference resources relative to their own location.

Kotlin achieves this via `getCodeExamplesAsTests()` which creates a temp directory and copies
context. The TypeScript test runner mirrors this with a `createChildContext` call that overrides
both paths.

**For the Go implementer:** The test harness needs the same two-directory setup. Don't assume
`scriptDir` and `tempDir` can be the same as the spec file's directory — temp files created
by tests would pollute the spec directory.

## 5. Shell command: environment variable serialization

The Shell command exports all script variables as environment variables. Non-string values must
be serialized — Kotlin uses `toDisplayYaml()` which produces YAML-formatted strings (objects
become YAML maps, arrays become YAML sequences).

The TypeScript implementation matches this, but the serialization format matters: downstream
scripts that parse these env vars need to handle YAML, not JSON. Using `JSON.stringify()` would
produce valid but incompatible output.

**For the Go implementer:** Use the same YAML serialization for env var export. Not JSON.

## 6. Run script: auto-list iteration on input

When `Run script` receives an array as `input`, it iterates: running the target script once
per element with that element as `input`. This is a command-level auto-iteration, separate from
the runtime's auto-list dispatch (§1 of the Level 0 report).

The implementation must create a fresh child context for each iteration, sharing the session
but isolating variables. The results are collected into an array.

**For the Go implementer:** This is easy to get wrong. The child context must share `session`
(for side effects like Answers) but have its own `variables` map. Don't reuse the parent context.

## 7. Local file command resolution

When a command name isn't found in the registry, SpecScript scans `scriptDir` for matching
`.spec.yaml`/`.spec.md` files. The filename-to-command-name conversion: `create-greeting.spec.yaml`
→ `Create greeting` (hyphen to space, capitalize first letter).

Resolution order matters: `.spec.yaml` is preferred over `.spec.md`. And imported files (from
`specscript-config.yaml`) are scanned before the local directory.

**For the Go implementer:** Implement the resolution chain in this order: registry → imports →
local `.spec.yaml` → local `.spec.md`. The filename conversion is case-sensitive on the first
character only.

## 8. Markdown shell block defaults differ from YAML Shell command

When a `shell` Markdown block is converted to a Shell command, `show output` defaults to `true`.
But the YAML `Shell:` command defaults `show output` to `false`. This is intentional — Markdown
blocks show their output to make documentation readable; YAML commands are silent by default.

This default difference is easy to miss. The converter must explicitly set `show output: true`
when generating Shell commands from Markdown blocks.

**For the Go implementer:** Don't rely on the Shell command's default for Markdown-sourced
blocks. Set `show output: true` explicitly in the converter.

## Summary of recommendations

### For the Go implementer (priority order)

1. Test YAML block scalar behavior with strings that lack trailing newlines; apply conditional stripping if needed (§1)
2. Design the CLI entry point for in-process invocation from the start (§3)
3. Set up the two-directory context (scriptDir + SCRIPT_HOME) for spec.md test execution (§4)
4. Use YAML serialization (not JSON) for Shell env var export (§5)
5. Create fresh child contexts per Run script iteration, sharing session but not variables (§6)
6. Implement local file command resolution in the right order (§7)
7. Set `show output: true` explicitly for Markdown shell blocks (§8)

### For the SpecScript maintainer (priority order)

1. The YAML block scalar trailing newline issue is a latent portability bug — consider specifying the behavior (§1)
2. Cli command tests are brittle — they depend on exact CLI output formatting (§3)
3. The shell block `show output` default difference is implicit knowledge; consider documenting it in the Shell spec (§8)
