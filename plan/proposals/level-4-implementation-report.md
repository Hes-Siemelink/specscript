# Level 4 Implementation Report

Findings from the TypeScript Level 4 implementation (226/233 tests passing, 7 expected skips).

## Test Results

| Suite | Passed | Skipped | Failed |
|---|---|---|---|
| Level 0 | 37 | 1 | 0 |
| Level 1 | 64 | 3 | 0 |
| Level 2 | 33 | 0 | 0 |
| Level 3 | 47 | 3 | 0 |
| Level 4 | 45 | 0 | 0 |
| **Total** | **226** | **7** | **0** |

Remaining skips are Level 5+ dependencies (MCP server, Validate schema, Prompt).

## Scope

Level 4 adds HTTP capabilities: five HTTP verb commands (GET, POST, PUT, PATCH, DELETE),
session-scoped request defaults (Http request defaults), and an embedded HTTP server (Http server,
Http endpoint, Stop http server).

Three new source files: `http.ts` (verb commands + defaults), `http-client.ts` (sync HTTP
execution), `http-server.ts` (server lifecycle).

## 1. The deadlock problem: single-threaded HTTP server + synchronous client

**The fundamental architectural challenge of Level 4.** In Kotlin, Ktor/Netty runs the HTTP
server on background threads, so `runBlocking` in the HTTP client only blocks the calling
coroutine while server threads continue handling requests. In Node.js, everything runs on a
single event loop — a synchronous HTTP client call blocks the thread, which prevents the
server from processing the request, causing a deadlock.

**Solution: three-process architecture.**

1. **Parent process** (SpecScript engine) — runs synchronously, spawns child for server
2. **Child process** (HTTP server via `fork()`) — has its own event loop, handles HTTP
3. **Sub-subprocess** (for `script` handlers) — child spawns via `execFileSync`, imports
   the compiled engine from `dist/` to execute SpecScript commands

The parent uses `fork()` (not `spawn`) to get IPC for route registration. The child writes
a ready-signal file when listening; the parent polls with `existsSync()` (synchronous, no
event loop needed). Previous approaches with IPC messages + `spawnSync` busy-wait failed
because `spawnSync` blocks the event loop and prevents IPC message delivery.

**For the Go implementer:** Go has goroutines + channels, so this is trivial — run
`http.ListenAndServe` in a goroutine, use a channel for the ready signal, and make synchronous
HTTP calls from the main goroutine. No subprocess architecture needed.

**For the SpecScript maintainer:** The three-process architecture is the most complex part of
the TypeScript implementation. It works reliably but adds ~50ms overhead per server start (file
polling interval + process fork). If SpecScript ever supports async execution, the server could
run in-process and this complexity disappears.

## 2. HTTP client: synchronous fetch via child process

Node.js `fetch()` is async-only. To execute HTTP requests synchronously (as the SpecScript
engine requires), the client spawns a child process that runs fetch and writes the response to
a temp file. The parent reads the result synchronously.

The child process script is written to a temp file and executed with `execFileSync`. It receives
request parameters via a JSON file (not command-line args, to avoid shell escaping issues with
URLs and headers). The response file contains a JSON envelope with status, headers, and body.

**For the Go implementer:** `net/http` is synchronous by default. Just call `http.DefaultClient.Do(req)`.

## 3. Request parameter merging

HTTP verb commands accept either a simple URL string or an object with parameters. When
`Http request defaults` is active, parameters are merged:

- Scalar fields (url, username, password): request overrides defaults
- Nested objects (headers, query): deep-merged, request keys override default keys
- Body: request overrides defaults entirely (no merge)
- Path: appended to default URL with `/` separator

The merging logic lives in `mergeWithDefaults()` in `http-client.ts`. Kotlin's implementation
is in `HttpParameters.kt`.

**For the Go implementer:** The merge semantics are straightforward but the path-joining edge
case matters: `url: http://host` + `path: /api` should not produce a double slash.

## 4. HTTP response parsing

Response bodies are parsed as YAML first, falling back to raw string. This means a response
of `"42"` becomes the number `42`, and `"true"` becomes boolean `true`. This matches Kotlin's
`parseYamlIfPossible()` behavior.

Non-2xx responses throw `SpecScriptCommandError` with the HTTP status code as the error type
string (e.g., `"404"`). The error message is the response body.

## 5. Server handler types and the handler resolution chain

Endpoint handlers are determined by `parseHandlerData()`:

| Input | Handler type |
|---|---|
| String value | Script file reference |
| Object with `output` key | Output handler (variable interpolation) |
| Object with `script` key | Inline script handler |
| Object without output/script | Treated as inline output |
| Primitive (number, boolean) | Static output |

Output handlers resolve `${...}` variables using request context (`request.body`,
`request.headers`, `request.query`, `input`). Script handlers run SpecScript commands
with the request body as `input`.

**For the Go implementer:** The handler type detection order matters. Check for `output` key
first, then `script`, then fall back to inline output.

## 6. Server ready signal: file-based polling

The parent needs to know when the child's HTTP server is listening. Three approaches were tried:

1. **IPC message from child** — Failed because `spawnSync`-based polling blocks the event loop,
   preventing IPC message delivery
2. **Stdout from child** — Failed for similar reasons; synchronous reads block
3. **File-based ready signal** — Works: child writes a file when listening, parent polls with
   `existsSync()` in a tight loop with 10ms `Atomics.wait()` sleeps

The file approach is crude but reliable. The ready file is cleaned up on server stop.

**For the Go implementer:** Use a channel. `<-readyCh` is one line of code.

## 7. Node.js lowercases HTTP headers

`IncomingMessage.headers` lowercases all header names per Node.js convention. But SpecScript's
`request.headers` variable should preserve original casing (matching Kotlin/Ktor behavior).

Fix: use `req.rawHeaders` which is an alternating `[key, value, key, value, ...]` array
preserving original casing.

**For the Go implementer:** Go's `http.Request.Header` preserves casing (it's a
`map[string][]string` with canonical MIME header keys). No special handling needed.

## 8. Server child process: CommonJS module constraints

The server child process script must be CommonJS (not ESM) because it's written to a temp
file and executed with `fork()`. Node.js determines module type from the file extension and
`package.json` — a standalone `.js` file defaults to CommonJS.

This means the server script can't use `import` syntax. It uses `require('node:http')` etc.
For script handlers that need to run the SpecScript engine, the sub-subprocess uses
`--input-type=module` flag to enable ESM imports of the compiled `dist/` output.

**For the Go implementer:** Not applicable — Go compiles to a single binary.

## 9. Port conflicts from stale child processes

During development, crashed tests can leave child processes holding ports open. This manifests
as `EADDRINUSE` errors on subsequent test runs. The test runner's `afterAll` calls
`stopAllServers()` to clean up, but abnormal termination (e.g., Ctrl+C during tests) can
leave orphans.

Workaround: kill processes on test ports before running tests:
```bash
for port in 2525 25001 25002 25003 25004; do
  pid=$(lsof -ti :$port 2>/dev/null)
  [ -n "$pid" ] && kill $pid 2>/dev/null
done
```

**For the SpecScript maintainer:** Consider adding a port-cleanup step to the test runner's
`beforeAll` or using random ports for server tests to avoid conflicts entirely.

## Summary of recommendations

### For the Go implementer (priority order)

1. Use goroutines + channels for the HTTP server — no subprocess architecture needed (§1, §6)
2. Use `net/http` directly for synchronous HTTP client calls (§2)
3. Match the parameter merge semantics, especially path-joining (§3)
4. Parse response bodies as YAML first, fall back to string (§4)
5. Detect handler type in the correct order: output → script → inline output (§5)
6. Go preserves header casing; no special handling needed (§7)

### For the SpecScript maintainer (priority order)

1. The three-process architecture is the highest-complexity part of the TypeScript impl; async execution would eliminate it (§1)
2. File-based ready signaling is reliable but adds startup latency (§6)
3. Port conflicts from stale processes are a development friction issue (§9)
