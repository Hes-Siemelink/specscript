# Cd command

## Problem

Spec tests currently run in a temp directory so that `temp-file=` blocks and `yaml specscript` blocks share the same
directory for file resolution. This leaks `${SCRIPT_TEMP_DIR}` into code examples throughout the specification, which
confuses first-time readers. The temp directory is a test infrastructure detail, not a language concept.

To decouple `scriptDir` from `tempDir` in spec tests, we need a way for a `yaml specscript` block to say "resolve files
relative to this directory" — without hacking the test runner or adding synthetic internal commands.

## Proposed solution

Add a `Cd` command that changes `context.workingDir` for all subsequent commands in the same script scope.

### The command

```yaml
Cd: ${SCRIPT_TEMP_DIR}
```

Changes `workingDir` on the context. All commands that resolve files against `workingDir` (`Run`, `Read file`, `Shell`,
`SQLite`, `Store`) will use the new directory. The change persists for the remainder of the script (or block).

This is a value command — it takes a single path string. No object form needed.

### Markdown code block attribute

Support `cd=<dir>` on `yaml specscript` blocks:

````markdown
```yaml specscript cd=${SCRIPT_TEMP_DIR}
Run: my-script.spec.yaml
```
````

This is syntactic sugar. The parser injects a `Cd` command at the start of the block's command list, identical to
writing `Cd: ${SCRIPT_TEMP_DIR}` as the first line. The `cd=` attribute is invisible in rendered Markdown, which is the
primary use case — keeping spec code examples clean while controlling file resolution for tests.

### Implementation

1. **ScriptContext**: Change `workingDir` from `val` to `var`.
2. **Cd command**: New singleton in `specscript/commands/files/`, group `core/files`. Implements `ValueHandler`. Sets
   `context.workingDir = Path.of(resolved)`.
3. **Script.kt**: In the `SpecScriptYaml`/`HiddenSpecScriptYaml` branch of `toScript()`, check
   `block.getOption("cd")`. If present, prepend a `Cd` command to the block's command list.
4. **CommandLibrary**: Register `Cd`.
5. **TestUtil**: Revert to `FileContext(file)` (no temp directory). Spec files that need temp-file resolution add
   `cd=${SCRIPT_TEMP_DIR}` to their `yaml specscript` blocks.

### Scope

- The `Cd` command only affects `workingDir`. It does NOT change `scriptDir`, `scriptHome`, or `tempDir`.
- The `Cd` command does not create child scopes or save/restore state. It's a simple assignment.
- The existing `cd` property on `Run`, `Shell`, and `Cli` remains unchanged — those are per-invocation overrides.
