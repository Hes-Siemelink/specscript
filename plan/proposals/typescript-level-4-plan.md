# TypeScript Level 4 Implementation Plan: HTTP

## Scope

Level 4 turns SpecScript into an API testing and integration tool. Nine new commands cover
HTTP client operations (GET, POST, PUT, PATCH, DELETE), request defaults management, and
an embedded HTTP server for testing.

Additionally, two Level 3 test-runner bugs should be fixed first — they're false skips caused
by the spec.md pre-check not accounting for local file commands.

## Pre-work: Fix Level 3 false skips (2 tests)

Two tests are skipped because the test runner's `unavailable` check (line 269 of spec-runner.test.ts)
calls `getCommandHandler()` on the global registry, which doesn't know about local file commands.
These commands ("Goodbye", "Generate greeting") are created at runtime by Temp file and yaml file=
blocks, then resolved by the context's `findLocalFileCommand`.

**Fix:** The `unavailable` check should not flag commands that could plausibly be local file commands.
Options:
1. Remove the pre-check entirely — let the test run and fail naturally (noisy)
2. Only flag commands that start with an uppercase letter AND aren't registered (local file commands
   always start uppercase, but so do real commands — this doesn't help)
3. Track file= blocks as "commands that will be created" and exclude them from the unavailable check

Option 3 is cleanest but complex. The pragmatic fix: maintain an explicit skip-override set for
these 2 tests, or remove the pre-check for sections that follow a Temp file or yaml file= block.

Actually, the simplest fix: these tests fail the pre-check because the commands aren't in the
registry. But they WOULD work at runtime because the Temp file command creates the .spec.yaml file
and local file resolution finds it. Just remove these from the `unavailable` check — let them run.

## Implementation Order

### 1. HTTP client (5 commands + defaults + shared HttpClient)

All five HTTP verb commands delegate to a shared `HttpClient` module. Implement them together.

#### HttpClient module (new file: `commands/http-client.ts`)

Core function: `processRequest(method, data, context) → JsonValue`

- Merge request parameters with `Http request defaults` from `context.session`
- Build URL from `url` + `path` (with path parameter substitution)
- Set headers (merge defaults + request-level)
- Set cookies (merge defaults + request-level)
- Handle body: JSON serialization (default) or form-urlencoded (by Content-Type)
- Handle authentication: Basic auth (username + password)
- Execute request using `fetch()` (Node.js 18+ built-in) or `node:http`/`node:https`
- Parse response: try JSON/YAML, fall back to raw string
- Handle errors: non-2xx throws `SpecScriptCommandError` with status code as type
- Optional `save as`: stream response to file

#### HTTP verb commands (new file: `commands/http.ts`)

| Command | Value form | Object form |
|---------|-----------|-------------|
| GET | URL string | HttpParameters object |
| POST | URL string (no body) | HttpParameters object |
| PUT | — | HttpParameters object |
| PATCH | — | HttpParameters object |
| DELETE | URL string | HttpParameters object |

All delegate to `HttpClient.processRequest()` with the appropriate method.

#### Http request defaults (same file or `commands/http-defaults.ts`)

- Object form: stores parameters in `context.session['http.defaults']`
- Value form: retrieves current defaults
- Subsequent HTTP requests merge with these defaults

### 2. HTTP server (3 commands)

#### Http server (new file: `commands/http-server.ts`)

- DelayedResolver — endpoint handler content is resolved at request time, not at server start
- Creates an embedded HTTP server using Node.js `node:http` (or Express-like, but prefer stdlib)
- Server config: `name` (required), `port` (default 3000), `endpoints`
- Endpoint handlers: `output` (static value) or `script` (inline script or file path)
- Path parameter support: `:id` → extract from URL
- Request context variables exposed to handlers: `${input}`, `${request.headers}`,
  `${request.path}`, `${request.pathParameters}`, `${request.query}`,
  `${request.queryParameters}`, `${request.body}`, `${request.cookies}`
- Server registry: track running servers by name in a module-level map
- Multiple simultaneous servers supported

#### Http endpoint

- DelayedResolver
- Adds endpoints to the most recently started server (or named server)
- Same endpoint format as Http server

#### Stop http server

- Value form: server name string
- Stops the named server and removes from registry

### 3. Sample server for spec tests

The Kotlin test suite starts `samples/http-server/sample-server/sample-server.spec.yaml` as a
`@BeforeAll` fixture. This server runs on port 2525 and provides echo endpoints used by all
HTTP client spec tests.

The TypeScript test runner must do the same: start the sample server before HTTP tests, stop it
after. This requires the Http server command to be implemented first (chicken-and-egg with the
client tests).

Implementation order within Level 4:
1. Http server + Http endpoint + Stop http server (server-side first)
2. Start sample server in test runner
3. HTTP client commands (can now test against the sample server)
4. Http request defaults

### 4. Test file wiring

Add to `spec-runner.test.ts`:
- `LEVEL_4_TEST_FILES` with `Http client tests.spec.yaml` and `Http server tests.spec.yaml`
- `LEVEL_4_MD_FILES` with all 9 `.spec.md` files
- Sample server startup/shutdown in `beforeAll`/`afterAll`

## External Dependencies

- **HTTP client:** Node.js built-in `fetch()` (available since Node 18) — no external dependency
- **HTTP server:** Node.js built-in `node:http` — no external dependency
- **URL parsing:** Node.js built-in `URL` class

No new npm dependencies needed. This is a significant advantage over Kotlin (which uses Ktor
for both client and server).

## Test Expectations

### spec.yaml tests
- Http client tests: 9 tests (all use sample server on :2525)
- Http server tests: 4 tests (start/stop their own servers on :25xxx ports)

### spec.md tests (~30 sections across 9 files)
- GET.spec.md: 7 sections (use sample server)
- POST.spec.md: 3 sections
- PUT.spec.md: 2 sections
- PATCH.spec.md: 2 sections
- DELETE.spec.md: 3 sections
- Http request defaults.spec.md: 7 sections
- Http server.spec.md: 7 sections
- Http endpoint.spec.md: 4 sections
- Stop http server.spec.md: 1 section

## Risk: Port conflicts

The spec.md tests use hardcoded ports (25001-25012, 2525). If any port is in use, tests fail.
Kotlin has the same constraint. For CI, this is fine; for local development, port conflicts
are possible but unlikely on those high ports.

## Estimated scope

- ~300-400 lines for HTTP client module
- ~300-400 lines for HTTP server module
- ~100 lines for command handlers (thin wrappers)
- ~50 lines test runner changes
- Target: ~50 new tests passing
