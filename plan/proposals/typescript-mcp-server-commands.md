# TypeScript: Implement MCP Server Commands

## Problem

The TypeScript implementation of SpecScript is missing all six MCP commands: `Mcp server`, `Mcp tool`,
`Mcp tool call`, `Mcp prompt`, `Mcp resource`, and `Stop mcp server`. These are the largest unimplemented subsystem,
classified as Level 6 (independent module). The spec runner currently skips them via the
`HIGHER_LEVEL_COMMANDS` set. Implementing them unlocks 6 spec.md files and ~34 executable test blocks.

## Proposed Solution

Port all six MCP commands to TypeScript, following the existing HTTP server implementation
(`http-server.ts`) as the architectural template. Use the official `@modelcontextprotocol/sdk` npm package (currently
v1.29.0) for MCP protocol handling, mirroring the Kotlin implementation's use of the official Kotlin MCP SDK.

## Scope

### Commands to implement

| Command           | Role                                             | Kotlin source                 | Complexity |
|-------------------|--------------------------------------------------|-------------------------------|------------|
| `Mcp server`      | Start server with inline tools/resources/prompts | `McpServer.kt` (348 lines)    | High       |
| `Mcp tool`        | Add tools to running server                      | `McpTool.kt` (37 lines)       | Low        |
| `Mcp resource`    | Add resources to running server                  | `McpResource.kt` (33 lines)   | Low        |
| `Mcp prompt`      | Add prompts to running server                    | `McpPrompt.kt` (33 lines)     | Low        |
| `Mcp tool call`   | Client-side tool invocation                      | `McpToolCall.kt` (114 lines)  | Medium     |
| `Stop mcp server` | Stop server by name                              | `StopMcpServer.kt` (16 lines) | Trivial    |

### Files to create

- `typescript/src/commands/mcp-server.ts` — All six commands plus server lifecycle
- No need for separate files per command — Kotlin splits them but the TypeScript HTTP server groups related commands in
  one file, which is the better pattern for this codebase

### Files to modify

- `typescript/package.json` — Add `@modelcontextprotocol/sdk` dependency
- `typescript/src/commands/register.ts` — Register the six new commands
- `typescript/test/spec-runner.test.ts` — Add MCP spec files, remove from `HIGHER_LEVEL_COMMANDS` skip set

## Architecture

### Server registry (mirrors HTTP server pattern)

```typescript
const SESSION_KEY_DEFAULT_SERVER = 'mcp.server.default'
const servers = new Map<string, McpManagedServer>()

interface McpManagedServer {
  server: McpServerInstance  // from @modelcontextprotocol/sdk
  httpServer?: HttpServer    // node:http for HTTP/SSE transports
  transport: 'HTTP' | 'SSE' | 'STDIO'
}
```

### Transport support

Three transports, matching the Kotlin implementation:

1. **HTTP (Streamable HTTP)** — Default. Uses `@modelcontextprotocol/sdk`'s `StreamableHTTPServerTransport`
   behind a `node:http` server. Endpoint at `/mcp`.
2. **SSE** — Legacy. Uses `@modelcontextprotocol/sdk`'s `SSEServerTransport` behind a `node:http` server. Endpoint at
   `/mcp` (GET for SSE stream, POST for messages).
3. **STDIO** — Uses `StdioServerTransport`. Binds to `process.stdin`/`process.stdout`.

### Handler execution (shared with HTTP server)

The `runScriptHandler` function in `http-server.ts` already implements the output/script/script-file resolution pattern.
This should be extracted into a shared utility and reused by both HTTP and MCP commands, since the pattern is identical:

- If `output` is set → resolve variables and return
- If `script` is a string → load and run the `.spec.yaml` file
- If `script` is an object → treat as inline SpecScript commands and run

### DelayedResolver

All commands that define handlers (`Mcp server`, `Mcp tool`, `Mcp resource`, `Mcp prompt`) must use
`delayedResolver: true` to prevent eager variable resolution of script bodies. `Mcp tool call` also needs delayed
resolution for the `input` property. This matches the Kotlin implementation.

### Client-side (`Mcp tool call`)

Creates an ephemeral MCP client via the SDK, connects to the target server (HTTP, SSE, or STDIO transport), calls the
specified tool with optional arguments, and returns the result. The client is closed after the call.

Transport selection for the client side:

| YAML property             | Transport             |
|---------------------------|-----------------------|
| `url` with no `transport` | HTTP (default)        |
| `transport: SSE`          | SSE                   |
| `transport: HTTP`         | HTTP                  |
| `command`                 | STDIO (spawn process) |

### Input schema derivation

When a tool's `script` property references an external `.spec.yaml` file that contains an
`Input schema` or `Input parameters` command, the tool's JSON Schema should be automatically derived from that file (if
no explicit `inputSchema` is provided). This matches the Kotlin `deriveInputSchema()`
behavior.

## Implementation Phases

### Phase 1: Foundation

- [ ] Add `@modelcontextprotocol/sdk` to `package.json`
- [ ] Extract `runScriptHandler` from `http-server.ts` into a shared utility
- [ ] Create `mcp-server.ts` with server registry and `Mcp server` command (HTTP transport only)
- [ ] Implement `Stop mcp server`
- [ ] Implement `Mcp tool call` (HTTP transport only)
- [ ] Verify with the "Static output" end-to-end test from `Mcp server.spec.md`

### Phase 2: Modular commands

- [ ] Implement `Mcp tool` (add tools to running server)
- [ ] Implement `Mcp resource` (add resources to running server)
- [ ] Implement `Mcp prompt` (add prompts to running server)
- [ ] Implement inline script handlers (not just static output)
- [ ] Implement external script file handlers

### Phase 3: Transports and client

- [ ] Add SSE server transport
- [ ] Add STDIO server transport
- [ ] Add SSE client transport to `Mcp tool call`
- [ ] Add STDIO client transport to `Mcp tool call` (process spawning)
- [ ] Implement input schema derivation from external scripts

### Phase 4: Test integration

- [ ] Register all commands in `register.ts`
- [ ] Add MCP spec files to `spec-runner.test.ts`
- [ ] Remove `'Mcp server'` and `'Mcp tool'` from `HIGHER_LEVEL_COMMANDS`
- [ ] Add `stopAllMcpServers()` to test cleanup (mirrors `stopAllServers()` for HTTP)
- [ ] Run full specification test suite and fix any failures

## Considerations

### JSON serialization

Unlike Kotlin (which needs a Jackson ↔ kotlinx.serialization bridge), TypeScript uses native JS objects everywhere. The
MCP SDK also uses plain objects. No serialization bridge needed — this simplifies the port considerably.

### Dynamic key maps

Kotlin uses `@JsonAnySetter` for the dynamic tool/resource/prompt name maps. In TypeScript, the YAML data already
arrives as a plain object — just iterate `Object.entries(data)` to get name→info pairs.

### Test coverage

The spec files contain 34 executable blocks across 6 files. One block is marked FIXME (STDIO transport in
`Mcp tool call.spec.md` — hangs after HTTP client test). The TypeScript implementation should handle this the same way —
skip or fix.

### SDK version

The `@modelcontextprotocol/sdk` package is at v1.29.0. The Kotlin SDK is at v0.8.4. The TypeScript SDK is more mature —
its API may differ from what the Kotlin port uses. Verify the server-side API for tool/resource/prompt registration
matches the spec's expected behavior.
