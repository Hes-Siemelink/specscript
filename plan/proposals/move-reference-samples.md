# Move Reference Samples into Specification Directory

## Problem

The `specification/` directory depends on files outside its own tree:

1. **`samples/basic/`** — 4 spec files use `cd=samples` to run CLI commands against this directory. Three of them
   hardcode the exact directory listing output (the AGENTS.md "sensitive areas" warning). The "Organizing" spec even
   has a stale file listing that doesn't match reality.
2. **`samples/http-server/sample-server/`** — started by `SpecScriptTestSuite.kt` before all spec tests. ~30 spec
   files reference `localhost:2525` endpoints. Cannot run HTTP spec tests standalone with `spec --test`.
3. **`samples/hello.spec.yaml`** — referenced by `Running SpecScript files.spec.md` (which already creates its own
   copy via `file=` anyway).

This coupling means:
- Adding/removing files in `samples/basic/` silently breaks spec tests
- Spec tests can't run standalone — they require the Gradle/JUnit test harness for server launch and CWD
- `cd=samples` only works because the JVM CWD happens to be the repo root — it's an implicit convention, not
  an explicit path resolution

## Proposal

Two-phase approach: (1) inline what can be inlined, (2) move the rest.

### Phase 1: Inline where possible

Three spec files reference `samples/basic/` but don't inherently need shared files. They can create local test
fixtures with `file=` blocks instead.

| File | Current dependency | Change |
|---|---|---|
| `Organizing SpecScript files in directories.spec.md` | `cd=samples`, hardcoded listing (STALE) | Already uses `file=` blocks in 2nd half. Rewrite 1st half to create its own local directory with `file=` blocks. Remove `cd=samples` entirely. |
| `Run script.spec.md` | `file: samples/basic/create-greeting.spec.yaml` (line 83) | Already creates `file=create-greeting.spec.yaml` at line 38. Change line 83 to use that file instead. One-line fix. |
| `Cli.spec.md` | `cd: samples`, hardcoded listing (lines 44-59) | Create a local directory with 2-3 `file=` blocks. Change `cd:` to use local dir. |

After Phase 1, only **two files** still depend on `samples/basic/`:
- `Running SpecScript files.spec.md` — the "getting started" tutorial
- `Command line options.spec.md` — the CLI options reference

These two are the tutorial entry points that deliberately walk users through the `samples/` directory. They have
a legitimate reason to reference real, browseable sample files.

### Phase 2: Move reference samples

Move the files that spec tests need into `specification/`:

| Current location | New location |
|---|---|
| `samples/basic/` | `specification/samples/basic/` |
| `samples/hello.spec.yaml` | `specification/samples/hello.spec.yaml` |
| `samples/http-server/sample-server/` | `specification/samples/sample-server/` |

#### Naming: `specification/samples/`

Considered alternatives:
- `specification/example-code/` — too verbose for what appears in `spec --help`
- `specification/reference-code/` — sounds like API reference docs
- `specification/fixtures/` — too test-jargony
- `specification/samples/` — short, clear, matches user mental model

The `samples/` name is familiar from the repo root and reads naturally in docs:
"the specification's samples directory". It will appear in `spec --help specification` output,
which is fine — it's useful content.

If we don't want it in the help listing, add `hidden: true` to its `specscript-config.yaml`:

```yaml
Script info:
  description: Reference samples used by specification tests
  hidden: true
```

#### Path references after the move

The spec files that still reference samples will use `cd=specification/samples` (for `shell cli` blocks)
or `cd: specification/samples` (for `Cli:` commands). This works the same as `cd=samples` today — relative
to the repo root / JVM CWD.

Using `${SCRIPT_HOME}/../../samples` was considered but rejected:
- Fragile when specs move between directories
- Ugly in documentation
- No better than the current implicit convention
- There is no repo-root or spec-root variable (and adding one is out of scope)

#### `specscript-config.yaml` for `specification/samples/`

```yaml
Script info:
  description: Reference samples used by specification tests
  hidden: true
```

### Phase 3 (optional): Sample server self-launch

Currently, `SpecScriptTestSuite.kt` launches the sample server via `@BeforeAll`. This means HTTP spec tests
fail when run with `spec --test` because nothing starts the server.

**Option A: Use `Before all tests` blocks.** Each spec file (or a shared test config) could include:

```yaml
Before all tests:
  Http server:
    name: sample-server
    port: 2525
    endpoints: ...
```

Concerns:
- Duplicates the server definition across files, or requires a shared import mechanism
- Prevents parallel test execution (port conflict)
- Every test file that hits `localhost:2525` would need this setup

**Option B: Leave as-is.** The test harness launches the server. This is pragmatic and matches how real projects
work (external services are running). `spec --test` support for HTTP tests would require a separate solution
(e.g., a test bootstrap script concept).

**Recommendation: Leave as-is (Option B) for now.** The server launch is a test infrastructure concern. Moving
the server files into `specification/samples/sample-server/` still improves locality — the test harness just
points to a different path.

### Changes required

**Phase 1 — Inline (3 files):**

- `specification/language/Organizing SpecScript files in directories.spec.md` — rewrite lines 1-60 to use
  `file=` blocks; remove `cd=samples`; fix the stale file listing
- `specification/commands/core/files/Run script.spec.md` — change line 83 from
  `file: samples/basic/create-greeting.spec.yaml` to `resource: create-greeting.spec.yaml`
- `specification/commands/core/shell/Cli.spec.md` — create local directory with `file=` blocks; remove
  `cd: samples`

**Phase 2 — Move (after Phase 1 is tested):**

- `git mv samples/basic specification/samples/basic`
- `git mv samples/hello.spec.yaml specification/samples/hello.spec.yaml`
- `git mv samples/http-server/sample-server specification/samples/sample-server`
- Update `specification/cli/Running SpecScript files.spec.md` — change `cd=samples` to
  `cd=specification/samples`
- Update `specification/cli/Command line options.spec.md` — same
- Update `specification/commands/core/connections/Connect to.spec.md` — update prose link
- Update `src/tests/specification/specscript/spec/SpecScriptTestSuite.kt` — update `SAMPLE_SERVER` path
- Update `typescript/test/spec-runner.test.ts` — update sample server path
- Update `AGENTS.md` — remove the sensitive areas warning about `samples/basic/`
- Update `README.md` — update any sample references

**Phase 2 does NOT move** the remaining `samples/` content (`digitalai/`, `goals-app/`, `spotify/`,
`home/`, `markdown/`, `mcp/`, `programming/`, `shell/`, `test/`, `ticket-db/`). Those are illustrative
user-facing examples, not spec test fixtures.

## What stays the same

- No functional behavior changes — only file locations and documentation paths
- The `samples/` directory continues to exist for illustrative samples
- The sample server launch mechanism stays in the test harness
- `cd=` resolution semantics are unchanged (relative to CWD)

## Execution order

1. Phase 1 first — it's independently valuable (fixes the stale listing bug, reduces coupling from 6 files to 2)
2. Phase 2 after Phase 1 is tested and committed
3. Phase 3 is deferred — tracked separately if desired
