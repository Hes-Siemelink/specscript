# Http and Mcp sessions — implementation plan

Plan for [plan/ideas/sessions.md](../ideas/sessions.md). Supersedes the *implicit reuse* design in
[mcp-client-state.md](mcp-client-state.md) (human decision: explicit sessions only), but reuses its verified research:
the `Mcp-Session-Id` wire behavior, the Kotlin SDK 0.8.4 findings (no reconnect-by-id; the live `Client`/transport
object must stay alive), and the `context.session` propagation analysis (`Run.kt:65`, `FileContext.kt:40` share the
session map by reference).

## Decisions made (with the author, 2026-09-04)

1. **Explicit sessions only.** No implicit connection reuse. A bare `Mcp call tool` with `server:` stays
   connect-per-call, exactly as today.
2. **Ambient + session property.** The most recently opened session is the *current* one; bare `GET`/`POST`/`Mcp call
   tool` use it. All of these commands also accept `session: <name>` to target a specific open session.
3. **Http session = named defaults + lifecycle.** No cookie jar, no dedicated client per session in this iteration.
   OAuth works via headers/token in the session parameters.
4. **Migration via alias scaffolding, removed in the same effort.** See Migration section.
5. **Punt on the `server` vs root-level discrepancy** between `Mcp call tool` and Http commands (per sessions.md) —
   revisit in second review.

## Command design

### Http session

Object with the same fields as `Http request defaults` today (`url`, `path`, `body`, `headers`, `cookies`,
`save as`, `username`, `password`) plus `name`. If `name` is omitted, one is generated (`http-session-NNN`).
Opening a session makes it the current one. The command's output is the resolved session object (so `As:` captures
it).

```yaml
Http session:
  name: backend
  url: http://localhost:2525
  headers:
    Authorization: Bearer ${token}

GET: /items            # uses current session 'backend'

GET:
  path: /items
  session: backend     # explicit targeting
```

### Http close session

Value form: the session name. Object form: a session object (name is extracted). Sessions form a stack: closing the
current session makes the previously opened one current, or none. This plays well with nested scripts — a child
script that opens and closes its own session leaves the parent's current session intact.

### Mcp session

Object with the same fields as the `server` property of `Mcp call tool` (`transport`, `url`, `command`, `headers`,
`token` — i.e. `TargetServerInfo`) plus `name`. **Connects eagerly**: runs the `initialize` handshake at declaration
time and keeps the live SDK `Client` + transport in memory. This is what preserves the `Mcp-Session-Id` across tool
calls — the hard requirement for Playwright MCP. Output is the resolved session object; becomes the current MCP
session.

### Mcp close session

Value form: name. Object form: session object. Calls `client.close()` (which sends the transport's session
termination where applicable).

### Changes to existing commands

- `GET`/`POST`/`PUT`/`PATCH`/`DELETE`: resolve defaults from the targeted session (explicit `session:` property, else
  current session, else none). The existing `HttpParameters.create` merge logic is reused unchanged — only the source
  of the defaults object moves.
- `Mcp call tool` (and `Mcp read resource`, `Mcp get prompt`): gain `session: <name>`, mutually exclusive with
  `server:`. Resolution order: explicit `session:` → explicit `server:` (connect-per-call, unchanged) → current MCP
  session → error. Session calls reuse the live client and skip connect/close.

### Storage and lifecycle

- Kotlin: `context.session["http.sessions"]` — ordered map name → session data; `context.session["mcp.sessions"]` —
  ordered map name → live client + session data. "Current" = last entry (LinkedHashMap order gives the stack
  behavior). TypeScript: same keys in `context.session: Map<string, unknown>`.
- Because the session map propagates by reference through `Run` (inline and by file), sessions are visible in nested
  scripts for free.
- **Auto-close at script end**: `finally` in `Script.run()` (Kotlin, `language/Script.kt`) closes all open MCP
  clients; TypeScript equivalent in its runner. Http sessions need no close action beyond removal, but go through
  the same hook for symmetry.
- Crash cleanup (process killed mid-script → orphaned server-side session) stays unsolved, as in the earlier
  proposal.

## Migration: superseding Http request defaults

Impact measured: 64 occurrences in 23 files. The two structural couplings:

- **`Connect to`** is implemented and documented as sugar over `Http request defaults` (`ConnectTo.kt`,
  `Connect to.spec.md:17`). It must be reworked to open/configure an Http session instead.
- Three `specscript-config.yaml` files (connections tests, cloud-connector sample, release folders sample) embed
  `Http request defaults` as connection config.

Approach — **alias scaffolding, removed at the end of this effort**, so every commit is green:

1. Land `Http session` / `Http close session`; `Http request defaults` becomes a thin delegate that writes into an
   anonymous default session (same storage). All existing specs keep passing untouched.
2. Migrate specs, tests, and samples mechanically (`Http request defaults:` → `Http session:`). Watch the two
   non-mechanical spots: the eval form `/Http request defaults: {}` in
   `samples/digitalai/platform/credentials/login.spec.yaml:8`, and `Before all tests` usage in
   `Before all tests.spec.md` and `Http client tests.spec.yaml`.
3. Rework `Connect to` and the three config yamls to target `Http session`.
4. Delete `Http request defaults`: handler, schema, spec file, `CommandLibrary.kt` / `register.ts` entries,
   `levels.yaml` and `commands/README.md` references. Breaking change → `⚠️` commit.

## Work plan (spec-first, per sessions.md Approach)

### Phase 1 — Specs (no implementation)

Load `.agents/skills/specscript-specs` before writing. New files:

- `specification/commands/core/http/Http session.spec.md` + `schema/Http session.schema.yaml`
- `specification/commands/core/http/Http close session.spec.md` + schema
- `specification/commands/ai/mcp/Mcp session.spec.md` + `schema/Mcp session.schema.yaml`
- `specification/commands/ai/mcp/Mcp close session.spec.md` + schema

Updates: `GET`/`POST`/`PUT`/`PATCH`/`DELETE` spec files, `Mcp call tool.spec.md` (fold in the relevant parts of
`plan/draft-specs/Mcp call tool.spec.md`), `Connect to.spec.md`, `commands/README.md`, `levels.yaml`. Corner cases
(multiple sessions, explicit `session:` targeting, close-reopens-previous-current, session + per-request override
merge) go into `tests/Http client tests.spec.yaml` and a new `tests/Mcp session tests.spec.yaml`, not the spec
files.

**Testability of generated names**: the session object output contains a generated name when none is given. Assert
it with the partial-match form of `Assert that` (`item:`/`in:`, see the Contains section of
`Assert that.spec.md`) — assert the known parameters are `in` the output, leaving the generated name unasserted.

### Phase 2 — Superficial human review of the specs

### Phase 3 — Kotlin implementation

- `commands/http/HttpSession.kt`, `HttpCloseSession.kt`; refactor `HttpClient.kt:37-40` to resolve defaults via the
  session registry; alias in `HttpRequestDefaults.kt`.
- `commands/mcp/McpSession.kt`, `McpCloseSession.kt`; refactor `McpCallTool.kt` (and read resource / get prompt) for
  the session/server/current resolution order; keep live clients in the registry.
- Cleanup hook in `Script.run()`. Register commands in `CommandLibrary.kt`.
- Run `./gradlew specificationTest` until green.

### Phase 4 — TypeScript implementation

Mirror in `typescript/src/commands/http.ts` / `http-client.ts` / `mcp-server.ts`, register in `register.ts`, cleanup
hook in the TS runner. The TS SDK's `StreamableHTTPClientTransport` also holds the session id on the live transport —
same keep-alive approach. Run the TS spec-runner tests.

### Phase 5 — Human inspects, rewrites specs to taste; make everything work again

### Phase 6 — Migration and removal

Steps 2–4 of the Migration section above. Final `⚠️` commit removes `Http request defaults`.

## Risks and open points

- **Test isolation** (inherited, not introduced): `TestCaseRunner` shares one `FileContext` — and thus one session
  map — across all test cases in a spec file. A session opened in one test case leaks into the next. Decision: keep
  behavior as-is in this pass. If flaky errors show up, consider introducing a `Before each test` command later
  (deliberately out of scope now).
- **Stdio MCP sessions** hold a live subprocess — treat as a special case with dedicated logic: auto-close at script
  end is mandatory, and the close command must terminate the process.
- **Current-session semantics across `Run`**: nested scripts see and can change the current session of the parent
  (shared map). Documented behavior, same as `Http request defaults` today.
- **`Mcp server` name collision**: `Mcp session` names live in a separate registry from `Mcp server` names — no
  interaction.
- Second-review items (punted): `server`-vs-root shape consistency between Mcp and Http commands; cookie jar /
  per-session HTTP client; opt-out/fresh-connection forms.
