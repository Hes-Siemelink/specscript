# Session support for Mcp read resource and Mcp get prompt

Implements [plan/proposals/mcp-sessions-read-resource-get-prompt.md](../proposals/mcp-sessions-read-resource-get-prompt.md).
The two commands now resolve a target like `Mcp call tool` does: explicit `session:` → `server:` (connect-per-call) →
current open session → error. `session` and `server` remain mutually exclusive.

## Test results

- Kotlin: `./gradlew specificationTest` — 576 tests, 0 failures, 0 skipped. `./gradlew test` green.
- TypeScript: `npx vitest run` — 571 passed, 6 skipped (all pre-existing known skips), 0 failures.
- New tests all execute in both implementations:
  - `Mcp read resource tests.spec.yaml`: "Read resource in the current session", "Read resource targeting a
    specific session"
  - `Mcp get prompt tests.spec.yaml`: "Get prompt in the current session", "Get prompt targeting a specific session"
  - `Mcp read resource.spec.md` / `Mcp get prompt.spec.md`: new `## Sessions` sections (one test each per spec file,
    heading-named "Sessions").

## What changed

Specification:
- Both schemas: added `session`, removed `server` from `required`, added `not: { required: [server, session] }`
  (matching `Mcp call tool.schema.yaml`).
- Both spec docs: new `## Sessions` sections mirroring `Mcp call tool.spec.md`.

Kotlin:
- New `McpTarget.kt` — sealed `McpTarget` (Session/ConnectPerCall) + shared `resolveMcpTarget()` that centralizes the
  resolution order, mutual exclusion, and error messages. `McpCallTool.kt` refactored onto it (behavior unchanged,
  spec tests prove it).
- `McpReadResource.kt` / `McpGetPrompt.kt`: `server` now nullable, added `session`; split into
  session-reuse (`readResource`/`getPrompt` on the live client) and connect-per-call (`...Once`) paths.

TypeScript:
- `resolveMcpTarget` parametrized with the command name so the "either session or server" error names the right
  command.
- `McpReadResourceCommand` / `McpGetPromptCommand` refactored to resolve the target; session path reuses
  `session.client`, per-call path keeps connect/close. Matches `McpCallToolCommand` structure.

## Observations / things to check in the diff

- **Error wrapping on a malformed `server`** differs slightly from before: if `server` is present but not an object,
  TS now wraps the `CommandFormatError` in the command's `SpecScriptCommandError` (e.g. ``Resource 'x' read failed:
  ...``). This is exactly what `McpCallToolCommand` already does, so the three commands are now consistent. No spec
  test covers the malformed-server case; flagging it in case it matters.
- **`plan/TODO.md`** is left untouched even though it lists "Rename MCP tool to name and input to argument" —
  already done in a prior commit. Not my scope.
- The pre-existing TS `tsc` errors (mcp-server.ts:287/590/600, cli.ts, package-registry.ts) are unrelated to this
  work — verified against a clean stash, present on `main`.

## Deferred (not in scope)

- The stateful-server TODO in `Mcp session tests.spec.yaml` — needs a stateful mock server. The new explicit-session
  tests prove the session plumbing against stateless servers.