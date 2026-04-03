# Shell/CLI Working Directory and Naming Cleanup

**Status**: Proposal
**Addresses**: `plan/proposals/shell-cli-cleanup-request.md`

## Problem

The `` ```shell cli`` and `` ```shell`` markdown directives have inconsistent defaults that are surprising in
practice.

`` ```shell cli`` defaults to a random temp directory. This forces 15 spec blocks to add `cd=${SCRIPT_HOME}` just to
run a script that lives next to the current file — the most natural thing to do.

`` ```shell`` defaults to the process CWD — wherever the user launched `spec` from. This contradicts how everything
else in markdown works: links, images, and file references all resolve relative to the document.

There is no `${PWD}` variable, so once we fix the defaults there is no way to explicitly request process CWD.

The `` ```shell cli`` name is misleading: it does not run a shell. It runs SpecScript in-process. The `shell` prefix
was chosen for syntax highlighting in markdown renderers.

The `` ```yaml file=`` directive silently writes to a temp directory, but the name suggests writing a file in place.

## Current Defaults

| Directive / Command | Default working directory |
|---|---|
| `` ```shell `` (markdown) | Process CWD |
| `` ```shell cli `` (markdown) | Temp directory |
| `Shell: { command: ... }` (YAML) | Process CWD |
| `Shell: { resource: ... }` (YAML) | Script directory |
| `Cli: ...` (YAML) | Temp directory |

## Proposed Changes

### 1. Default working directory → SCRIPT_HOME (markdown directives and YAML Cli)

Change `` ```shell``, `` ```shell cli``, and YAML `Cli:` to default to `SCRIPT_HOME` — the directory of the script
being executed. YAML `Shell:` keeps its current defaults (revisit after this refactoring with more real-world
examples).

**Why SCRIPT_HOME is the right default:**

- Markdown links resolve relative to the document. Shell and Cli commands should follow the same principle.
- `Shell: { resource: ... }` already defaults to the script directory. The `resource` property was introduced
  specifically because the CWD default was inconvenient. The same reasoning applies to all shell/cli invocations.
- Real-world usage runs scripts from the project, not from a temp directory. The temp dir pattern is a documentation
  testing artifact.

**Impact on specification files:**

- 15 blocks with `cd=${SCRIPT_HOME}` can drop the `cd=` option.
- 36 blocks that rely on the temp dir default would need `cd=${SCRIPT_TEMP_DIR}` added. However, these all pair
  with `` ```yaml file=`` blocks — and that directive is getting renamed to `` ```yaml temp-file=`` (see item 5),
  which makes the temp-dir dependency explicit anyway.
- 4 blocks that already have `cd=${SCRIPT_TEMP_DIR}` are unaffected.
- 4 flag-only blocks (`spec --help` etc.) are unaffected.

The net result is more explicit code across the board: every block that operates on temp-created files says so
explicitly with `cd=${SCRIPT_TEMP_DIR}`.

### 2. Introduce `${PWD}` variable

Add a built-in variable `${PWD}` that resolves to the process's current working directory (the directory from which
`spec` was launched).

This is the escape hatch for the rare case where you want process CWD after the default changes to SCRIPT_HOME.
Use `cd=${PWD}` to opt in.

Implementation: set in `FileContext.init` alongside `SCRIPT_HOME`. Also inject as env var in Shell subprocesses
(they already inherit OS `PWD`, but a SpecScript variable makes it usable in `cd=${PWD}`).

### 3. Rename `` ```shell cli`` to `` ```cli``

`` ```shell cli`` does not run a shell. It runs SpecScript in-process via `Cli.kt`. The `shell` prefix was chosen
for syntax highlighting, but `` ```cli`` renders fine as a plain code block.

Replace `` ```shell cli`` with `` ```cli``. No alias, no transition period — remove the old form entirely.

### 4. Rename `` ```yaml file=`` to `` ```yaml temp-file=``

The current name suggests writing a file in place. It actually writes to a temporary directory
(`SCRIPT_TEMP_DIR`). Renaming makes this explicit: when you see `temp-file=`, you know the file lives in a temp
directory and any command that needs it must run there.

Replace `file=` with `temp-file=`. No alias — remove the old form entirely.

This rename pairs naturally with change #1: when a `` ```cli`` block follows a `` ```yaml temp-file=`` block, the
`cd=${SCRIPT_TEMP_DIR}` annotation reads as self-evident rather than mysterious boilerplate.

### 5. Fix SCRIPT_HOME env var inconsistency in Shell subprocess

In `Shell.kt`, the `SCRIPT_HOME` environment variable injected into subprocesses uses `context.scriptDir`:

```kotlin
info.env["SCRIPT_HOME"] = context.scriptDir.toAbsolutePath().toString()
```

The SpecScript *variable* `${SCRIPT_HOME}` uses `context.scriptHome`. During markdown spec test execution, these
differ: `scriptDir` points to a temp directory, `scriptHome` points to the real file location.

This means `${SCRIPT_HOME}` in SpecScript resolves to the real location, but `$SCRIPT_HOME` inside a shell
subprocess resolves to the temp dir. This is a bug.

Fix: use `context.scriptHome` in both `Shell.kt` and TypeScript `shell.ts`.

This bug is independent of the other changes — it exists regardless of default working directory.

## Summary

| # | Change | Breaking? |
|---|---|---|
| 1 | Markdown Shell and Cli default working dir → SCRIPT_HOME | Yes — spec files need updating |
| 2 | Introduce `${PWD}` variable | No |
| 3 | `` ```shell cli`` → `` ```cli`` (remove old form) | Yes — all spec files updated |
| 4 | `` ```yaml file=`` → `` ```yaml temp-file=`` (remove old form) | Yes — all spec files updated |
| 5 | Fix SCRIPT_HOME env var in Shell subprocess | Bug fix |

YAML `Shell:` command keeps its current defaults. Revisit after this refactoring with more examples.

## Execution Order

1. Introduce `${PWD}` variable — provides escape hatch before anything breaks
2. Fix SCRIPT_HOME env var bug — independent fix
3. Replace `` ```shell cli`` with `` ```cli`` + replace `` ```yaml file=`` with `` ```yaml temp-file=``
4. Change Shell and Cli markdown defaults to SCRIPT_HOME + update spec files (add `cd=${SCRIPT_TEMP_DIR}` where
   needed, drop `cd=${SCRIPT_HOME}` where it was required before)

Steps 1-2 can ship independently. Steps 3-4 are the breaking changes and ship together with the documentation
migration.
