# Proposal: `Run` command (replaces `Run script`)

## Problem

There is no way to run a block of SpecScript commands with a different `scriptDir`. The `Do` command executes commands
in the current context, inheriting `scriptDir`. The `Shell` command has a `cd` property, but that only affects shell
execution — not SpecScript file resolution.

This matters because file-oriented commands (`Run script`, `Read file`, `Write file`, etc.) resolve paths relative to
`scriptDir`. When you need to work with files in a different directory — a temp directory, a subdirectory, or an
external project — you have no clean way to scope a block of commands to that directory.

Meanwhile, the `script` property pattern (string = file path, object = inline commands) already exists in `Http server`
and `Mcp server` via `HandlerInfo`, but isn't available as a standalone command.

## Proposed solution: `Run` command

Rename `Run script` to `Run` and extend it into the unified script execution command. It graduates from a utility
command to a central language feature that handles all script execution patterns:

- Run a script file (existing)
- Run a script file from a different directory (new: `cd`)
- Run inline commands in a different directory (new: `script` + `cd`)

`Run script` remains as an alias for backward compatibility.

### Schema

```yaml
oneOf:
  # Value form: Run: foo.yaml
  - type: string

  # Object form
  - type: object
    properties:
      script: { type: [object, string] }  # inline commands or file path
      file: { type: string }              # resolve relative to workingDir
      cd: { type: string }                # override scriptDir for file resolution
      input: { }                          # input parameters
    additionalProperties: false
```

Script source — exactly one of `script`, `file`, or the value form:

| Form | Resolves relative to |
|---|---|
| `Run: foo.yaml` | `scriptDir` |
| `script: foo.yaml` | `scriptDir` (or `cd` if set) |
| `script: { Print: Hello }` | Inline commands, file ops use `cd` (or `scriptDir`) |
| `file: foo.yaml` | `workingDir` (or `cd` if set) |

`script` (string) without `cd` resolves relative to `scriptDir` — the same as the old `resource` property, which it
replaces. The value form (`Run: foo.yaml`) is shorthand for `script: foo.yaml`.

`file` takes an absolute or CWD-relative path. This pairs naturally with `Temp file`, which returns the absolute path
of the created file — use `file` to run a script you just created as a temp file.

When `cd` is present, it overrides the base directory for file resolution. `cd` is resolved in the parent context
(so `${SCRIPT_TEMP_DIR}` works).

### Examples

Running a script file:

```yaml
Run: hello.spec.yaml
```

Running a script file from a different directory:

```yaml
Run:
  cd: ${SCRIPT_TEMP_DIR}
  script: hello.spec.yaml
```

Running inline commands in a different directory:

```yaml
Run:
  cd: ${SCRIPT_TEMP_DIR}
  script:
    Print: Hello
    Read file: data.json
```

Running a script file with input:

```yaml
Run:
  script: create-greeting.spec.yaml
  input:
    name: Alice
```

Running a script created with `Temp file`:

```yaml
Temp file:
  filename: hello.spec.yaml
  content:
    Output: Hello!

Run:
  file: ${output}
```

`Temp file` returns the absolute path of the created file. `file` accepts absolute paths, so the two compose directly.

## Isolation ladder: `Do` — `Run` — `Cli`

The three execution commands form a ladder of increasing isolation:

| | `Do` | `Run` | `Cli` |
|---|---|---|---|
| Context | Same (shared) | New `FileContext` | Separate CLI invocation |
| Variables | Shared with parent | Clean scope (`input` only) | None (separate process) |
| Commands | From host script | From host script | From target file's directory |
| `scriptDir` | Inherited | `cd` or target file's dir | Target file's directory |
| Use case | Inline block | Script execution | Full isolation |

- **`Do`** — bare-metal inline execution. `data.run(context)`. No isolation, no ceremony. Use for control flow.
- **`Run`** — execute a script (file or inline) with variable isolation but within the host script's ecosystem.
  The host script's local commands, imports, and connections remain available. Use for composing scripts.
- **`Cli`** — execute a script as a separate CLI invocation with full isolation. The target script sees only its own
  directory's commands. Use when you need process-level separation.

## Command resolution in `Run`

All forms of `Run` use the host script's command resolution. Whether the target is inline commands, a temp file, or a
script in a subdirectory — these are all part of the host script's ecosystem and should see its commands.

The `Temp file` case makes this clear: the file contents were written two lines above. It's conceptually inline code
that happens to live in a file. It would be surprising if it lost access to sibling scripts just because it was
written to disk first.

The edge case — running an unrelated file from a different project that accidentally picks up host commands — is not a
realistic problem. Standard commands take precedence (checked first in `getCommandHandler`), and if someone needs true
isolation from the host, `Cli` exists for exactly that.

Implementation: the child `FileContext` delegates `getCommandHandler()` to the parent context. The `scriptDir` (used
for file path resolution) points to `cd` or the target file's directory, but command lookup falls through to the parent.

## Specification document

The isolation ladder (`Do` — `Run` — `Cli`) warrants its own spec file:
`specification/language/How to run SpecScript code from within SpecScript.spec.md`

This document introduces the three commands as a progression, explains when to use which, and links to the individual
command specs for details. The existing `Do.spec.md`, `Run script.spec.md` (renamed to `Run.spec.md`), and
`Cli.spec.md` remain as command reference pages.

## Design notes

### Implementation

- Rename `RunScript` to `Run` (keep `Run script` as alias in `CommandLibrary`).
- `script` (string) without `cd`: resolve relative to `scriptDir`, delegate to `SpecScriptFile.run()`. Replaces the
  old `resource` property.
- `script` (string) with `cd`: resolve relative to `cd`, delegate to `SpecScriptFile.run()`.
- `script` (object): create a child `FileContext` with `scriptDir` pointing to `cd` (or current `scriptDir` if no
  `cd`). Command resolution delegates to the parent context (see above). Needs `DelayedResolver` for the `script`
  property only.
- `file` with or without `cd`: resolve relative to `cd` (or `workingDir` if no `cd`).
- `cd` is resolved in the parent context before creating the child context.
- `resource` remains as a deprecated alias for `script` (string form) for backward compatibility.
- `HandlerInfo.run()` (used by `Http server` and `Mcp server`) could delegate to `Run` internally, unifying the
  `script` property dispatch logic.

### What changes in FileContext

Nothing. Standard `FileContext` construction with different paths. No mutable overrides, no scoping functions.

### Variables and context

- Child context inherits `session` from parent (shared state).
- Variables are a clean scope — `input` is passed when provided; empty otherwise.
- `cd` is resolved in the *parent* context.
- `script` block runs in the *child* context.
- Output of the last command becomes the output.

## Previous attempt: `cd=` code block attribute (reverted)

A `cd=` attribute on `yaml specscript` code blocks was implemented and reverted. It introduced a synthetic
`__with_script_dir__` command in the core execution loop and mutable state in `FileContext`. Making this an official
command avoids both problems — spec examples use real SpecScript, and the execution model stays clean.
