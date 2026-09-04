# MCP client connection state

A general proposal for `Mcp call tool` to optionally keep a connection alive across calls, instead of always
connecting and disconnecting per call. Motivated by driving Playwright MCP (see
[playwright-sketch.md](playwright-sketch.md)), but this is not a Playwright feature — any MCP server that keeps
connection-scoped state hits the same problem, and the fix belongs at the `Mcp call tool` level. Playwright shouldn't
need to appear in the actual command spec; see [the draft spec](../draft-specs/Mcp%20call%20tool.spec.md) for what
that would look like kept generic.

## The problem

`McpCallTool.kt` connects, calls one tool, and closes — for every single call. That's fine for a stateless server
(most `Mcp server`-defined demo/test servers are). It breaks down for a server that relies on the connection itself
to carry state.

That's not a workaround some servers do — it's built into the protocol. MCP's Streamable HTTP transport has an
explicit `Mcp-Session-Id` header: the server may hand one out on `initialize`, and the client is expected to carry it
on every subsequent request. We verified this directly, by hand, with curl against a running Playwright MCP server:
`initialize` returns the header, every `tools/call` after that carries it back, and a server-side idle timeout or an
explicit `DELETE` ends it. Playwright MCP uses that session to hold the actual browser page. Throw the session away
between calls (which is what happens today) and you get a fresh blank tab per call — verified live, including with
`--shared-browser-context` enabled, which doesn't help.

## What the SDK actually holds

Checked directly against `kotlin-sdk` 0.8.4 sources (not assumed):

- `StreamableHttpClientTransport.sessionId` (`private set`, populated from the response header in `send()`) — the
  server-correlation token.
- `Client` (extends `Protocol`): `serverCapabilities`, `serverVersion`, `serverInstructions`, populated only by
  actually running the `initialize` handshake in `connect()`. `Protocol` itself holds `transport: Transport?` plus
  `_requestHandlers` / `_notificationHandlers` / `_responseHandlers` maps — live, in-memory request/response
  correlation state, not something a session-id string can reconstruct.

There is no supported "reconnect with just the ID" path — no constructor or setter to inject a known session ID into
a fresh transport, no `Client.attach(...)`. The wire protocol itself doesn't care (a raw HTTP client presenting the
right header from an entirely separate process works fine — we proved that with curl), but the Kotlin SDK's public
API doesn't expose that shortcut. Conclusion: the only way to keep a session alive is to keep the live
`Client`/transport object alive in memory and keep using it.

## Design: implicit, keyed connection reuse

Rather than requiring an explicit start/stop around every use, `Mcp call tool` reuses a connection automatically
when the same server is targeted again — no new syntax required for the common case.

- Key the reuse by full connection identity: url + transport + headers + token together, not url alone. Url alone is
  wrong — two calls to the same url with different credentials must never share a connection.
- Store the live connection in `ScriptContext.session: MutableMap<String, Any?>` — this already exists, already
  serves as a generic cross-command state bag, and already has exactly the right lifetime semantics (see below). No
  new context plumbing needed.
- On a call: compute the key, look it up in `context.session`. Found → reuse, skip `connect()`. Not found → connect,
  then store it instead of closing it.
- Close everything left in `context.session` when the top-level script run finishes (same place `Script.run()`
  already wraps stdout capture — add a `finally` there). Automatic sessions never need an explicit stop.

### The "scripts calling scripts" question is already answered

Checked `Run.kt` and `FileContext.kt` rather than assume an answer:

- `Run.kt:65` (inline `Run: {script: ...}`): `session = context.session` — same map object.
- `FileContext.kt:40` (`Run: {file: ...}`, a separate `.spec.yaml`): `parent.session` — same map object again.
- Only `FileContext.clone()` (used elsewhere, e.g. test-case isolation) copies it.
- The single-arg `FileContext(scriptFile)` — what a top-level `spec somefile.spec.yaml` invocation gets — is where a
  fresh, empty session map is actually born.

So "one script run" is already a real boundary in the existing code, and it already propagates by reference through
every `Run`, inline or by file, arbitrarily deep. A script that calls another script that calls the same MCP server
gets the same implicit session automatically, for free, with zero new mechanism — this falls out of code that exists
for unrelated reasons.

### Explicit override: `Mcp start session` / `Mcp stop session`

For when the default (one implicit connection per distinct server identity) isn't what you want — two independent
connections to the same server, or a session whose lifetime you want visible and deliberate rather than incidental:

- `Mcp start session: {name, server}` — connects, stores under an explicit name instead of the connection-identity
  key.
- `Mcp stop session: <name>` — closes it early. Same shape as the existing `Stop mcp server`, which already takes a
  plain string name.
- `Mcp call tool` gains `session: <name>` as an alternative to `server: {...}` (mutually exclusive — exactly one of
  the two).

## Test runner implications

Checked `TestUtil.kt` rather than assume test cases already get a clean slate: they don't, not even today.
`TestCaseRunner` builds **one** `ScriptContext` per spec file (`FileContext(file, workingDir = scriptDir)`) and reuses
it across every dynamic test case generated from that file — between cases it resets `context.error` and the `input`
variable, nothing else. `context.session` already carries over between test cases in the same file, for whatever's
already stored there (e.g. `connect-to.overrides`).

So an implicit MCP session carrying over between test cases in the same spec file isn't new complexity this proposal
introduces — it's consistent with how everything else in `context.session` already behaves. Existing MCP
specification tests pass today because the demo/mock servers they hit are stateless. A genuinely stateful test
scenario would hit the same cross-test-case sharing that any other `context.session` state already has. If that
turns out to matter, the fix arguably belongs at the test-runner level — reset or clone `session` between test
cases — rather than inside the MCP feature itself.

## Open questions

- **Connection-identity key**: url + transport + headers + token, as above — needs to actually be implemented as a
  proper composite key (e.g. a hash), not just url, or two calls with different credentials will silently share a
  connection.
- **Opt-out**: no way yet to force a guaranteed-fresh connection when one to that identity already exists (e.g.
  deliberately wanting a second, independent browser tab without naming a session for it). Maybe `session: none`.
  Leave for whoever actually needs it.
- **Crash cleanup**: if the process dies mid-script, the `finally` on `Script.run()` never runs and the server-side
  session (and whatever it holds — a browser tab, a subprocess, a lock) is orphaned. Same caveat already noted in
  `playwright-sketch.md`; not solved here either.
- **Test isolation**: see above — flagging it, not fixing it in this proposal.
