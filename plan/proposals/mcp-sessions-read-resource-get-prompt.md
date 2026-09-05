# Session support for `Mcp read resource` and `Mcp get prompt`

## Problem

[MCP sessions](http-mcp-sessions.md) already exist and are wired into `Mcp call tool` only. The other two MCP client
commands that can target a server — `Mcp read resource` and `Mcp get prompt` — only support connect-per-call via a
`server:` block. They cannot use an open [Mcp session](https://github.com/example) nor target one by name. This is an
inconsistency: session-aware MCP servers (those that keep state on the connection, notably Playwright) that expose
resources or prompts have no way to read them against a live session.

## Current state (verified)

`Mcp call tool` is the reference pattern, implemented identically in both languages. Session resolution order:
explicit `session:` → explicit `server:` (connect-per-call) → current open session → error. `session` and `server` are
mutually exclusive.

- **Kotlin** — `src/main/kotlin/specscript/commands/mcp/McpCallTool.kt:24-48`: reads `CallMcpToolInfo.session`, uses
  `McpSession.sessions` registry for named/current lookup, calls `callTool(session.client, ...)` to reuse the live
  client, or `callToolOnce(info)` for connect-per-call. `CallMcpToolInfo` carries `server` and `session` as nullable
  fields; schema `not: { required: [server, session] }`.
- **TypeScript** — `typescript/src/commands/mcp-server.ts:749-801`: `resolveMcpTarget(data, context)` returns the
  session or `undefined`; empty/invalid `server` throws, else reuse `session.client`. `McpSessionCommand`/
  `McpCloseSessionCommand` and `mcpSessionRegistry` are already shared (`McpSessionEntry.client: Client`).

`Mcp read resource` and `Mcp get prompt` don't do any of this. They always build a fresh transport, `connect()`, call
once, and `close()`:

- `McpReadResource.kt:14-48` — `ReadMcpResourceInfo(uri, server)`; `server` is non-nullable, no session.
- `McpGetPrompt.kt:19-57` — `GetMcpPromptInfo(name, server, arguments)`; `server` non-nullable, no session.
- TypeScript `McpReadResourceCommand` (`mcp-server.ts:847-878`) and `McpGetPromptCommand` (`mcp-server.ts:882-917`) —
  same shape: mandatory `server`, always connect/close.

Their schemas (`Mcp read resource.schema.yaml`, `Mcp get prompt.schema.yaml`) require `server` and have no `session`
property; only `Mcp call tool` has the `session` property + `not: { required: [server, session] }`.

## Proposed solution

Mirror the `Mcp call tool` pattern exactly on both commands and in both languages. No new mechanism — reuse the existing
`McpSession` registry (Kotlin) / `mcpSessionRegistry` + `resolveMcpTarget` (TypeScript).

- Add `val session: String? = null` to `ReadMcpResourceInfo` and `GetMcpPromptInfo` (Kotlin); read `data.session` in the
  two TypeScript commands.
- Add the `session` property to both schemas and change `required: [server, ...]` to `not: { required: [server,
  session] }`, matching `Mcp call tool.schema.yaml`.
- Resolve target with the same order (explicit `session:` → `server:` → current → error), and reuse the live
  `session.client` when a session is resolved; otherwise current connect-per-call behavior is unchanged.
- Error message text should match the `Mcp call tool` wording where applicable
  ("No open Mcp session: …", "No MCP server specified and no open Mcp session").

### Kotlin specifics

In both `McpReadResource.kt` and `McpGetPrompt.kt`, replace the `data.toDomainObject(...)` → straight `readResource`/
`getPrompt` flow with the same `when (session/current)` branch and the "either session or server, not both" guard used
in `McpCallTool.kt:27-39`. Factor the target resolution into a small shared helper to avoid duplicating the branch in
three commands.

### TypeScript specifics

Extract a shared helper equivalent to `resolveMcpTarget` semantics (it is already defined for `Mcp call tool` at
`mcp-server.ts:780`); reuse it for all three commands. The two target commands currently take `data.server` directly —
switch them to resolve the session first (as `McpCallToolCommand` does) and call the SDK method against
`session.client` or a connect-per-call client.

## Scope boundaries

- **In:** `Mcp read resource`, `Mcp get prompt` — spec (`.spec.md` + schema + tests), Kotlin, TypeScript.
- **In:** a small shared session-resolution helper if it keeps the three commands consistent and DRY.
- **Out:** the existing "stateful server" TODO in `Mcp session tests.spec.yaml` (needs a stateful mock server) — this
  proposal only wires sessions into the two commands, it doesn't add stateful test infrastructure.
- **Out:** `Mcp prompt` / `Mcp resource` (the server-side declarations), `Mcp tool`, `Mcp server`, `Stop mcp server` —
  not client calls.
- **Out:** implicit connection reuse / opt-out forms (per the explicit-sessions decision in http-mcp-sessions.md).

## Impact

- Spec files: `Mcp read resource.spec.md`, `Mcp get prompt.spec.md` (add a Sessions section like
  `Mcp call tool.spec.md:115-154`), both schemas, `tests/Mcp read resource tests.spec.yaml`, `tests/Mcp get prompt
  tests.spec.yaml` (add session cases), and optionally the shared `tests/Mcp session tests.spec.yaml`.
- Kotlin: `McpReadResource.kt`, `McpGetPrompt.kt`, likely a small new shared helper file.
- TypeScript: `mcp-server.ts` (both commands + shared resolver).
- `CommandLibrary.kt` / `register.ts`, `levels.yaml`, `commands/README.md` — only if a new command is added (likely
  none; this only extends two existing commands).

## Open questions

- Should the current `server`-required schema be relaxed to allow `session` while keeping `required: [server]` for pure
  `server:` calls, or switch fully to `not: { required: [server, session] }` as `Mcp call tool` does? Recommend the
  latter for consistency. ANSWER: Yes.

- Testability of a genuinely stateful session-read still blocked on a stateful mock server (see TODO in `Mcp session
  tests.spec.yaml`). Recommend wiring optional explicit-session cases (target by name, current-session default) against
  the existing stateless test servers, which prove the plumbing without needing state. ANSWER: Yes