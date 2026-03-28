# Move Reference Samples into Specification Directory

## Problem

The `samples/` directory mixes two kinds of content:

1. **Reference samples** — test fixtures used by spec tests. `samples/basic/` is hardcoded into 3+ spec files.
   `samples/http-server/sample-server/` is started before every spec test run. These are part of the specification
   infrastructure.
2. **Illustrative samples** — standalone examples like `spotify/`, `goals-app/`, `digitalai/` that show real-world
   usage. These aren't referenced by spec tests (except `digitalai` which will become a module example).

Having reference samples in `samples/` creates a fragile coupling: the AGENTS.md warns that touching `samples/basic/`
breaks 3 spec files. The warning exists because the relationship is non-obvious — specification tests shouldn't depend
on files outside the `specification/` directory.

## Proposal

Move reference samples into the `specification/` directory tree, co-located with the specs that use them.

### What moves

| Current location | New location | Reason |
|---|---|---|
| `samples/basic/` | `specification/samples/basic/` | Used by 3 spec files via `cd=samples` |
| `samples/http-server/sample-server/` | `specification/samples/sample-server/` | Test fixture started by `SpecScriptTestSuite.kt` |

### What stays in `samples/`

Everything else: `digitalai/`, `goals-app/`, `spotify/`, `hello.spec.yaml`, `home/`, `markdown/`, `mcp/`,
`programming/`, `shell/`, `test/`, `ticket-db/`.

These are illustrative samples for users to explore. They may eventually move or be reorganized, but that's out of
scope here.

### Module spec samples

For the new Modules spec (`Modules.spec.md`), sample module directories will live at
`specification/language/module-samples/` (or a similar path relative to the spec document). These are real directories
with real `specscript-config.yaml` and `.spec.yaml` files that readers can inspect on GitHub and that spec tests
execute against.

### Changes required

**Spec files to update (3 files with `cd=samples`):**

- `specification/language/Organizing SpecScript files in directories.spec.md` — change `cd=samples` to
  `cd=specification/samples`
- `specification/cli/Command line options.spec.md` — same
- `specification/cli/Running SpecScript files.spec.md` — same

**Spec file with direct file reference:**

- `specification/commands/core/files/Run script.spec.md` — update path
  `samples/basic/create-greeting.spec.yaml` → `specification/samples/basic/create-greeting.spec.yaml`

**Test infrastructure:**

- `src/tests/specification/specscript/spec/SpecScriptTestSuite.kt` — update `SAMPLE_SERVER` path

**Documentation:**

- `AGENTS.md` — remove the "sensitive areas" warning about `samples/basic/` (the coupling becomes obvious once
  they're co-located)
- `README.md` — update any references to `samples/basic/`

### What this does NOT change

- The `samples/` directory continues to exist for illustrative samples
- No functional behavior changes — only file locations
- The `cd=` directives in spec files just point to a different path

## Scope

This is a housekeeping change. It can be done independently of the module system work, but it's convenient to do
it first since the module spec needs its own sample directory in `specification/`.
