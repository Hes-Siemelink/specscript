# Http and Mcp sessions — TypeScript port (Phase 4)

Report for `plan/proposals/http-mcp-sessions.md`. Phases 1 (specs) and 3 (Kotlin) were completed earlier; this
report covers the TypeScript port (Phase 4) plus a status snapshot of the whole effort.

## Test results

- Kotlin: `./gradlew check` green (specificationTest + unit tests).
- TypeScript: `npx vitest run` green — 564 passed, 6 skipped (pre-existing skips). Before this port, 24 tests failed
  with `Unknown command: Http session`.
- Manual CLI smoke tests: script using `Mcp session` + `Mcp call tool` runs and exits cleanly, both with explicit
  `Mcp close session` and with auto-close at script end (session left open).

## What was ported

- `language/sessions.ts` — `Session` / `SessionRegistry` / `closeAllSessions`, mirroring Kotlin `language/Sessions.kt`
  (stack semantics, `current`, name generation with counter, replace-same-name-close).
- `commands/http-sessions.ts` — `Http session` / `Http close session`.
- `commands/http-client.ts` — session defaults resolution in `processObjectRequest`: explicit `session:` name →
  current session → legacy `http.defaults`. The `session` property is consumed (deleted) like Kotlin.
- `commands/mcp-server.ts` — `Mcp session` (connects eagerly, keeps live `Client` + transport → preserves
  `Mcp-Session-Id`), `Mcp close session`, and `Mcp call tool` refactored to session/server/current resolution order.
  `session` and `server` are mutually exclusive.
- `commands/register.ts`, `test/spec-runner.test.ts` (close sessions after last test per file), `src/cli.ts`
  (`executeFile` closes sessions when `parent === undefined`, mirroring `SpecScriptCli.run`).

## Notes for the reviewer

- The running-Mcp-session keep-alive is exercised only indirectly (call-tool-in-session works, server responds to a
  second call on the same client). The "server-side session state survives across calls" guarantee is still untested —
  the corner-case spec file carries a TODO for a stateful test server (`Mcp session tests.spec.yaml:1`). Same gap in
  Kotlin.
- Punt markers preserved: `Http session.spec.md` still has three `yaml FIXME specscript` blocks (current-session
  semantics) deferred to the human spec rewrite (Phase 5). Their behavior is covered by the corner-case tests.
- `npx tsc --noEmit` reports 6 pre-existing type errors; none are introduced by this port.
- Pre-existing divergence (not touched): structured spec tests share the session map across test cases via
  `sharedContext.clone()`; flat test cases in TS get a fresh context each.

## Status of the overall plan

- Phase 1 Specs — done. Phase 2 Human spec review — pending. Phase 3 Kotlin — done, green.
- Phase 4 TypeScript — done, green (this report).
- Phase 5 Human rewrites specs to taste — pending.
- Phase 6 Migration (Http request defaults → Http session, then removal) — pending. Breaking change, `⚠️` commit.