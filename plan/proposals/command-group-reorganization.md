# Command Group Reorganization — Proposal

Date: 2026-09-25
Status: Proposed — awaiting review

## Goal

Give `specification/commands/` (and the Kotlin command namespaces that mirror it) a
logical, predictable grouping of all SpecScript commands. Only the final
reorganization is in scope here — the earlier moves (top-level `db`, `http`,
`mcp`, `shell`, new `core/run`) are already committed and serve as the starting
point.

## Guiding principle

Every command a user needs to **write down a spec and run it** lives under
`core/`. Everything that hands off to something *outside the SpecScript world* —
an OS process, a REST/HTTP service, an MCP server, a database, or the credential
store backing those — is a top-level integration group.

Crisp test: *does the command stay entirely within SpecScript's own data, files,
and scripts?* Yes → `core/`. If it touches the outside world → top-level.

This is a guiding principle, not a hard wall. Known leaky spots are called out
below and given a decision.

## Current state

| Group | Commands | Count |
|---|---|---|
| core/script-info | Script info, Input schema | 2 |
| core/variables | As, Output (`${}` assignment is a language feature) | 2 |
| core/data-manipulation | Add, Add to, Append, Fields, Values, Find, Json patch, Replace, Size, Sort | 10 |
| core/control-flow | Do, If, When, For each, Repeat, Exit | 6 |
| core/errors | Error, On error, On error type | 3 |
| core/testing | Tests, Before all tests, After all tests, Test case, Code example, Assert equals, Assert that, Expected output, Expected output contains, Expected console output, Expected error, Answers | 12 |
| core/user-interaction | Prompt, Confirm | 2 |
| core/schema | Validate schema | 1 |
| core/types | Check type | 1 |
| core/util | Json, Text, Print, Print Json, Parse Yaml, Base64 encode, Base64 decode, Wait | 8 |
| core/files | Read file, Write file, Temp file, Cd | 4 |
| core/run | Run, Cli (+ feature "SpecScript files as commands") | 2 |
| core/connections | Connect to, Credentials, Create credentials, Delete credentials, Get credentials, Get all credentials, Set default credentials | 7 |
| shell | Shell | 1 |
| http | GET, POST, PUT, PATCH, DELETE, Http session, Http close session, Http server, Http endpoint, Stop http server | 10 |
| mcp | Mcp server, Mcp tool, Mcp call tool, Mcp read resource, Mcp get prompt, Mcp session, Mcp close session, Mcp prompt, Mcp resource, Stop mcp server | 10 |
| db | SQLite, SQLite defaults, Store | 3 |

~84 commands + 2 language features. Shared core schemas (`Conditions.schema.yaml`,
`ParameterData.schema.yaml`) live at `core/` root and stay there.

## Issues in the current layout

1. **`core/connections` is the one real core-leak.** Credentials and `Connect to`
   exist purely to configure external endpoints (HTTP/MCP/db). Under the guiding
   principle they belong top-level next to `http`/`mcp`/`db`.
2. **`core/schema` vs `core/types` is a split mess, and the namespaces don't
   match.** `Validate schema` registers `core/schema` and lives in `core/schema/`;
   `Check type` *also* registers `core/schema`, but its spec/tests live in
   `core/types/` — and its Kotlin file sits in `commands/types/` while declaring
   `package specscript.commands.schema`. Both commands answer "is my data shaped
   correctly?" and should be one group.
3. **`core/util` is a grab-bag** (converters + console print + Wait). Acceptable
   as the misc/formatting bucket, but worth splitting `conversion` out if it
   grows.
4. **`http` and `mcp` each mix client and server.** `http` = 5 HTTP verbs +
   2 session commands + 3 server commands; `mcp` = 5 client ops + 5 server
   definitions. The README already presents them as client/server sub-sections.
5. **Documentation drift:** `commands/README.md` is missing Fields, Values,
   Validate schema, and Check type. `CommandLibrary.kt` groups `Find` under a
   "Control flow" comment while it registers `core/data-manipulation`.
6. **Minor:** `levels.yaml` still lists a non-existent
   `commands/mcp/tests/Mcp client tests.spec.yaml` at line ~408.

## Proposed target structure

```
specification/commands/
  README.md                      # rewritten to match below

  # Core — write down a spec and run it
  core/
    script-info/                 # Script info, Input schema
    variables/                   # As, Output, ${} assignment
    data-manipulation/           # Add, Add to, Append, Fields, Values, Find,
                                 #   Json patch, Replace, Size, Sort
    control-flow/                # Do, If, When, For each, Repeat, Exit
    errors/                      # Error, On error, On error type
    testing/                     # Tests, Before/After all tests, Test case,
                                 #   Code example, Assert equals/that, Expected …,
                                 #   Answers
    types/                       # MERGE of schema + types: Check type, Validate schema
    user-interaction/            # Prompt, Confirm
    util/                        # Json, Text, Print, Print Json, Parse Yaml,
                                 #   Base64 encode/decode, Wait
    files/                       # Read file, Write file, Temp file, Cd
    run/                         # Run, Cli, "SpecScript files as commands"

  # Top-level integrations — outside the SpecScript world
  connections/                   # MOVED from core/: Connect to, Credentials,
                                 #   Create/Delete/Get/Get all/Set default credentials
  shell/                         # Shell (spawns OS processes)
  http/
    client/                      # GET, POST, PUT, PATCH, DELETE,
                                 #   Http session, Http close session
    server/                      # Http server, Http endpoint, Stop http server
  mcp/
    client/                      # Mcp call tool, Mcp read resource, Mcp get prompt,
                                 #   Mcp session, Mcp close session
    server/                      # Mcp server, Mcp tool, Mcp resource, Mcp prompt,
                                 #   Stop mcp server
  db/                            # SQLite, SQLite defaults, Store
```

## Changes and rationale

### 1. Move `core/connections` → top-level `connections/` (recommended)

Crisp distinction the rest of the proposal builds on: **`core/run/Cli` is a
self-reference (runs the `spec` tool itself); `shell` and `connections` reach
outside.** Credentials are account/endpoint plumbing for integrations — same
family as http/mcp/db.

- Move `core/connections/` → `connections/`.
- Kotlin namespace in `ConnectTo.kt` / `Credentials.kt` and friends:
  `"core/connections"` → `"connections"`.
- `lines.yaml` keeps listing them — the previous sweep was mechanical; these
  entries need updating (`commands/core/connections/…` → `commands/connections/…`).

### 2. Merge `core/schema` + `core/types` → `core/types` (recommended)

- Move `core/schema/Validate schema.spec.md`,
  `core/schema/schema/ValidateSchema.schema.yaml`, `core/schema/tests/Validate
  tests.spec.yaml` into `core/types/`.
- Namespace for both `CheckType.kt` and `ValidateSchema.kt`: `"core/types"`.
- Kotlin: single package `specscript.commands.types`; move the `ValidateSchema.kt`
  file into `commands/types/`; fix `CheckType.kt`'s declared package.
- Alternative name considered: `core/validation`. `core/types` wins because the
  existing `Types.spec.md` / `Type tests.spec.yaml` already live there and "Check
  type" is the canonical command name.

### 3. Split `http` and `mcp` into `client/` + `server/` (recommended, optional)

- Namespaces become `http/client`, `http/server`, `mcp/client`, `mcp/server`.
- Moves each command's `.spec.md`, `schema/*.schema.yaml`, and relevant tests.
- Matches how the command reference already presents them; makes the
  client/server symmetry explicit.
- More invasive than 1–2, so it can be deferred or dropped without affecting the
  rest. Note: it will touch many cross-references in `commands/README.md` and the
  overview docs.

### 4. Keep `core/util` as-is (recommended)

Renaming or splitting it (e.g. `core/conversion`) creates 1–2 command groups for
no real clarity gain today. Revisit only if it grows.

### 5. Fix documentation drift (always)

- `commands/README.md`: add Fields, Values, Validate schema, Check type; align
  Data validation / client-server headings with the new layout.
- `CommandLibrary.kt`: move `Find` under the data-manipulation comment (cosmetic).
- `levels.yaml`: drop the stale `Mcp client tests.spec.yaml` entry.

### 6. Keep group names (recommended)

`script-info`, `data-manipulation`, `control-flow`, `user-interaction`, `util`
are consistent, if long. No rename without a stronger reason.

## Impact checklist (when approved)

- `specification/commands/` moves (connections, schema↔types, optional http/mcp split).
- Kotlin: `CommandLibrary.kt` imports, namespaces in the ~20 moved handlers,
  package renames in `commands/schema/` + `commands/types/`.
- `specification/levels.yaml`: path updates for moved files.
- `specification/commands/README.md`: rebuild group index.
- Docs that link moved spec files (`specification/overview/`,
  `specification/language/`, `specification/cli/`) — grep for the old paths.
- The spec test runner is fully recursive (`SpecScriptTestSuite.kt`), so no test
  wiring changes; `./gradlew specificationTest` + `compileKotlin` are the
  verification gates.

## Open questions

1. Include the optional `http`/`mcp` client-server split now, or defer?
2. Confirm `core/types` as the merged group name (vs `core/validation`)?
3. Confirm `connections` as the top-level group name?
4. Keep `core/util` as the misc bucket (no split), agreed?