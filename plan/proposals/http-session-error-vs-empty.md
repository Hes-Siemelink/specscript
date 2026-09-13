# Http session value form — error vs empty return

Findings from a session on the login.spec.yaml guard, in the context of the **Remove Http request defaults**
migration (`http-mcp-sessions.md` Phase 6). The design question below was deliberately deferred — the current task is
migrating samples away from `Http request defaults`, not cleaning up `Http session` error semantics. Pick this up in
a fresh session when that migration is done.

## Problem

`login.spec.yaml` starts with the "Do not reconnect if logged in already" guard. Under `Http request defaults` this
was probing `${output}` via the eval form `/Http request defaults: { }` — an empty result meant "not configured yet".
That trick does not port to `Http session`:

- `Http session: ""` throws `SpecScriptCommandError("No open Http session")` when there is no current session, so the
  probe errors instead of evaluating empty.
- `Http session: <name>` (the scoped form, preferred because the login must not pull the ambient default session)
  throws when the named session is not open.

## Chosen approach (working, this session) — `On error` + `SpecScriptCommandError`

The Kotlin `SessionRegistry.setCurrentSession` now throws `SpecScriptCommandError("No session with name '$name' found.")`
instead of `IllegalArgumentException` (`src/main/kotlin/specscript/language/Sessions.kt:44` — user change this
session; previously an internal error that `On error` cannot catch). The guard is:

```yaml
Http session: digital-ai-platform
As: ${session}

On error:
  ${session}: { }

If:
  not:
    empty: ${session}
  then:
    Exit: Already logged in
```

Why it works: `SpecScriptCommandError` lands in `context.error` (`Script.kt:60-67`), non-`ErrorHandler` commands are
skipped until `On error` runs, `On error` sets `${error}` / clears `context.error`, so `As: ${session}` then assigns
`{ }` and execution continues. Verified with the rebuilt jar, both branches:

- fresh run → "No session, continuing to login" (falls through to login)
- session already open → "Already logged in"

**TypeScript parity was applied this session**: `typescript/src/language/sessions.ts` `setCurrentSession` now throws
`SpecScriptCommandError` (imported from `language/types.js`) instead of plain `Error`. Previously the plain `Error`
was wrapped as an internal error and invisible to `On error`, so the guard would have failed in TS. Vitest green
(565 passed / 6 skipped). Note: Kotlin is the reference; the TS change mirrors it 1:1 including the message
`No session with name '$name' found.`

## Deferred alternative — value form returns an empty session `{}` instead of throwing

Proposed at end of session, not implemented. Argument: cheaper to check for empty than to catch an error, matches the
"missing things evaluate to empty" SpecScript philosophy, and no spec/​test pins the throwing behavior (grep for
`No open Http session` / `No session with name` in `specification/` returns nothing).

Proposed semantics for `Http session` value form (`src/main/kotlin/specscript/commands/http/HttpSession.kt:21-36`):

| Input   | Session state          | Returns                                                       |
|---------|------------------------|---------------------------------------------------------------|
| `""`    | current open           | current data (current unchanged)                              |
| `""`    | none                   | `{}` (current unchanged)                                      |
| `"name"`| open                   | its data, and it becomes current                              |
| `"name"`| not open               | `{}` (current unchanged)                                      |

The guard would then drop `On error`:

```yaml
Http session: digital-ai-platform
As: ${session}

If:
  not:
    empty: ${session}
  then:
    Exit: Already logged in
```

Constraints and consequences if picked up:

- **`As.kt:10` rejects null output** ("Can't assign output variable because it is empty."), so the empty return must
  be an empty object `{}`, not `null`/no output.
- **Keep the asymmetry**: `Http session: "name"` (a query) returns empty; targeting a `session:` on a request
  (`HttpClient.kt:43-51`) still errors — a request cannot proceed without defaults.
- Changes needed: Kotlin `HttpSession.kt` value form (check `get()` before `setCurrentSession()`; return `{}`);
  spec prose in `Http session.spec.md` (resolves the `<!-- TODO Add error behavior -->` and null test-case TODOs);
  `tests/Http session tests.spec.yaml` cases for missing name / empty string with no current; TS mirror in
  `commands/http-sessions.ts`.
- With this design, `setCurrentSession`'s `SpecScriptCommandError` becomes unreachable from `Http session` (harmless
  defense) — do NOT revert it meanwhile, the `On error` guard depends on it.

## Note for the migration

Deprecation of `Http request defaults` is tracked in `http-mcp-sessions.md` (impact: 64 occurrences in 23 files;
`Connect to` is sugar over `Http request defaults`; three `specscript-config.yaml` files embed it). The
`login.spec.yaml:8` eval-form probe is one of the two non-mechanical migration spots called out there and is already
rewritten to the `On error` form above. Remaining `Http request defaults` usages in `login.spec.yaml:37-48` (the
token and bearer branches) are mechanical replacements:
`Http request defaults: {url, headers}` → `Http session: {name: digital-ai-platform, url, headers}`.