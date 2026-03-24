# Proposal: MCP Server / HTTP Server Consistency

## Problem

The MCP server and HTTP server commands evolved independently and have diverged in naming, schema structure, lifecycle
management, request context variables, handler types, and implementation patterns. This makes the language harder to
learn and the codebase harder to maintain.

## Current Differences

### 1. Server Identity and Registry

| Aspect              | Http server              | Mcp server              |
|---------------------|--------------------------|-------------------------|
| Registry key        | `port` (Int)             | `name` (String)         |
| Identity property   | `port` (required)        | `name` (required)       |
| Name support        | None                     | Yes                     |
| Version support     | None                     | Yes (`version`)         |
| Stop mechanism      | `port` + `stop: true`    | `name` + `stop: true`   |

HTTP server has no `name` — you identify it solely by port. MCP server has a `name` (required) and an optional `port`
(defaults to 8080). This means you can't refer to an HTTP server by name, and you can't easily manage multiple servers
in a script.

### 2. Handler Structure

| Aspect               | Http server                        | Mcp server                          |
|----------------------|------------------------------------|-------------------------------------|
| Handler container    | `endpoints` → path → method → handler | `tools` / `resources` / `prompts` → name → handler |
| Handler types        | `output`, `script`, `file`         | `script` only (string = file)       |
| Static output        | `output` property                  | Not supported                       |
| File reference       | `file` property or string shorthand| String value in `script` property   |
| Inline script        | `script` property (object)         | Object value in `script` property   |

HTTP server distinguishes three handler types explicitly (`output`, `script`, `file`). MCP server overloads the
`script` property — if it's a string it means a file reference, if it's an object it means an inline script.

The MCP approach is more concise but less explicit. The HTTP approach is more explicit but more verbose.

### 3. Request Context Variables

| Variable               | Http server                | Mcp server                         |
|------------------------|----------------------------|-------------------------------------|
| `${input}`             | Body or query params       | `request.arguments` (tool only)     |
| `${request.headers}`   | Yes                        | No                                  |
| `${request.path}`      | Yes                        | No                                  |
| `${request.pathParameters}` | Yes                   | No                                  |
| `${request.query}`     | Yes                        | No                                  |
| `${request.queryParameters}` | Yes                  | No                                  |
| `${request.body}`      | Yes                        | No                                  |
| `${request.cookies}`   | Yes                        | No                                  |

HTTP server provides a rich `${request}` object. MCP server only sets `${input}` (for tools and prompts), with no
`${request}` equivalent. This is partly justified — MCP is a protocol-level abstraction, not raw HTTP — but the
inconsistency means script authors need to learn different variable models.

### 4. Modular Definitions

| Aspect                | Http server           | Mcp server              |
|-----------------------|-----------------------|-------------------------|
| Modular add commands  | None                  | `Mcp tool`, `Mcp resource`, `Mcp prompt` |
| Default server lookup | N/A                   | Session key `mcp.server.default` |
| Can add to running server | Only via repeated `Http server` call | Yes, via standalone commands |

MCP server supports defining tools/resources/prompts in separate commands after the server is created. HTTP server has no
equivalent — you must pass all endpoints in the `Http server` call (though you can call it again on the same port to add
more endpoints).

### 5. Implementation Patterns

| Aspect                | Http server                   | Mcp server                         |
|-----------------------|-------------------------------|-------------------------------------|
| Data class parsing    | Manual `data.getParameter()`  | `data.toDomainObject()`            |
| Endpoints parsing     | `Yaml.parse(data.getParameter("endpoints"))` | Via data class maps   |
| Server instance type  | `EmbeddedServer<Netty>` directly | MCP SDK `Server` + optional `EmbeddedServer<Netty>` |
| Keep-alive mechanism  | None (Ktor manages)           | Non-daemon thread with `Job.join()` |
| Logging               | `println("Starting...")`     | `println("Started...")`           |
| Shutdown grace period  | `stop(100, 200)`             | `stop(100, 200)` (same)           |
| Context cloning       | Per-request `clone()`         | Per-tool/resource/prompt `clone()` |
| Init block            | Disables Ktor shutdown hook   | Disables Ktor shutdown hook (same) |

Both share the Ktor shutdown hook workaround and 100/200ms grace period. But HTTP server uses manual property extraction
while MCP server uses `toDomainObject()` — the latter is cleaner and should be the standard pattern.

### 6. Schema Structure

| Aspect                        | Http server                       | Mcp server                      |
|-------------------------------|-----------------------------------|---------------------------------|
| Top-level required properties | None (port is technically optional in schema) | `name`                |
| `additionalProperties: false` | On root and MethodHandler         | On root and all definitions     |
| Handler type validation       | Union type `string \| object`     | Union type `string \| object`   |

Both use JSON Schema draft 2020-12 and both support union types for handlers. But HTTP server's schema doesn't mark
`port` as required even though the implementation reads it unconditionally.

### 7. Spec Document Style

| Aspect              | Http server                    | Mcp server                      |
|---------------------|--------------------------------|---------------------------------|
| Spec location       | `specification/commands/core/http/` | `specification/commands/ai/mcp/` |
| Separate test file  | Yes (`tests/Http server tests.spec.yaml`) | No (tests inline in spec) |
| Cleanup             | Hidden `stop` in HTML comments | Hidden `stop` in HTML comments (same) |
| Handler docs        | Detailed sections per handler type | Minimal, mostly tool-focused |

## Proposed Changes

### Phase 1: Low-Hanging Fruit (Non-Breaking)

These changes align patterns without breaking existing scripts.

**1a. Add `name` support to HTTP server** — Optional `name` property for HTTP server, allowing `stop` by name. Registry
becomes dual-keyed (port + optional name). This brings HTTP server closer to MCP server's lifecycle model.

**1b. Standardize implementation to `toDomainObject()`** — Refactor `HttpServer.kt` to use `data.toDomainObject()` for
parsing, matching the MCP server pattern. Replace the manual `data.getParameter()` / `Yaml.parse()` calls with a proper
data class.

**1c. Fix HTTP server schema** — Mark `port` as required in the schema (it already fails at runtime without it). Add
`additionalProperties: false` to the `Endpoint` definition for consistency.

**1d. Add HTTP server test file parity** — MCP server tests are inline in the spec. Not a consistency issue per se, but
worth noting for future alignment.

### Phase 2: Handler Unification (Minor Breaking)

**2a. Adopt MCP-style `script` overloading in HTTP server** — Allow `script: filename.spec.yaml` (string) as a shorthand
for `file: filename.spec.yaml`. This makes both servers use the same convention. Keep `file` as a deprecated alias.

**2b. Add `output` support to MCP server tools** — MCP tools currently require `script` for everything. Allow
`output: Hello World!` as a static response handler, like HTTP server has. This is particularly useful for mock servers
and simple tools.

**2c. Unified handler resolution** — Extract a shared handler resolution utility that both servers use. The logic
"if string → file, if object with `output` → static, if object → script" would be shared code.

### Phase 3: Modular Endpoint Definitions for HTTP (New Feature)

**3a. `Http endpoint` command** — Analogous to `Mcp tool`, allow defining endpoints after the server is created:

```yaml
Http server:
  name: my-api
  port: 8080

Http endpoint:
  /users:
    get:
      script: list-users.spec.yaml
```

This would use the same session-key mechanism as `mcp.server.default` to find the current server.

### Phase 4: Shared Server Infrastructure (Refactoring)

**4a. Extract `ServerRegistry<K, V>`** — A shared generic registry for both server types, handling start/stop/lookup.

**4b. Extract `ServerHandler` base** — Shared interface or abstract class with `execute()`, `stop()`, context cloning,
and keep-alive logic.

**4c. Shared Ktor lifecycle** — Both servers use Ktor/Netty. The MCP server has explicit keep-alive threading that the
HTTP server doesn't need (because Ktor's server already manages it). But the MCP keep-alive is there because the MCP SDK
server lifecycle is separate from Ktor's. This may not be fully unifiable, but the start/stop/grace-period code can be
shared.

## Recommendation

Start with **Phase 1** — it's all non-breaking, improves consistency, and cleans up technical debt. **Phase 2** is the
biggest user-facing improvement (unified handler model) but needs spec changes first. **Phase 3** is a new feature that
fills a gap. **Phase 4** is pure refactoring with no user-facing changes.

Phases 1 and 2 are independent and can proceed in parallel. Phase 3 depends on Phase 1a (name support). Phase 4 can
happen anytime but is most valuable after Phase 2.
