# Rename MCP client commands and add resource/prompt client commands

## Problem

1. `Mcp tool call` is the only MCP client command. There's no way to test resources or prompts from SpecScript.
2. The naming pattern `Mcp tool call` (noun-verb) is awkward and doesn't extend well to `Mcp resource read` or `Mcp prompt get`.

## Proposal

### Naming

Rename to verb-after-Mcp pattern for all client commands:

| Old name | New name | MCP protocol method |
|---|---|---|
| `Mcp tool call` | `Mcp call tool` | `tools/call` |
| *(new)* | `Mcp read resource` | `resources/read` |
| *(new)* | `Mcp get prompt` | `prompts/get` |

`Mcp tool call` remains as a deprecated alias to avoid a hard break. The schema file stays at `Mcp tool call.schema.yaml` and is referenced by both names.

### Command shapes

**Mcp call tool** (renamed from Mcp tool call, same schema):
```yaml
Mcp call tool:
  tool: greet
  input:
    name: Alice
  server:
    url: http://localhost:8094/mcp
```

**Mcp read resource**:
```yaml
Mcp read resource:
  uri: config://app
  server:
    url: http://localhost:8094/mcp
```

**Mcp get prompt**:
```yaml
Mcp get prompt:
  prompt: code-review
  input:
    code: "def foo(): pass"
  server:
    url: http://localhost:8094/mcp
```

All three commands use `input` (not `arguments`) to stay consistent with SpecScript's own vocabulary (`Input schema`, `Input parameters`, `${input.x}`).

### Spec changes

The main spec files (`Mcp call tool.spec.md`, `Mcp read resource.spec.md`, `Mcp get prompt.spec.md`) keep only the narrative: what the command does, basic usage, transport options. One or two executable examples each.

All combinatorial test cases (script vs output, arguments, variable resolution, error handling, etc.) move to `specification/commands/ai/mcp/tests/Mcp client tests.spec.yaml`. This file spins up one MCP server with tools, resources, and prompts, runs all cases, then tears down.

### Implementation

The `server` property and transport logic is identical across all three commands. The Kotlin `createMcpClient()` and TypeScript `createClientTransport()` functions are already extracted and reusable.

New commands need:
- **Mcp read resource**: call `client.readResource(uri)`, return first text content
- **Mcp get prompt**: call `client.getPrompt(name, arguments)`, return messages

### SSE transport retirement

MCP has officially deprecated SSE in favor of Streamable HTTP. Our spec already labels it "legacy". Drop SSE support:

- Remove SSE from the `transport` enum in schemas
- Remove SSE server/client code from both implementations
- Remove the SSE section from `Mcp server.spec.md`
- Only HTTP and STDIO transports remain

STDIO stays in the schema and implementation but isn't tested in the new test file (existing test is broken/FIXME).
The new client commands only test over HTTP.

### Test plan

All client command tests go in `specification/commands/ai/mcp/tests/Mcp client tests.spec.yaml`. The file sets up one
MCP server with tools, resources, and prompts, runs all test cases over HTTP, then tears down.

Tests are grouped by command:

**Mcp call tool:**
- Call tool with no arguments (static output)
- Call tool with input arguments
- Call tool with variable resolution in input
- Call tool backed by inline script
- Call tool backed by external script file
- Call tool with derived inputSchema (no explicit inputSchema on tool)

**Mcp read resource:**
- Read resource with static output
- Read resource backed by inline script
- Read resource backed by external script file
- Read resource with custom mimeType

**Mcp get prompt:**
- Get prompt with no input
- Get prompt with input arguments
- Get prompt with variable resolution in input
- Get prompt backed by inline script
- Get prompt backed by external script file

All tests use HTTP transport (the default). STDIO transport is tested separately in the `Mcp server.spec.md` transport
section — it requires spawning an external process and is orthogonal to the client command logic.

### Files affected

- `specification/commands/ai/mcp/Mcp tool call.spec.md` → rename to `Mcp call tool.spec.md`, trim to narrative
- New: `specification/commands/ai/mcp/Mcp read resource.spec.md`
- New: `specification/commands/ai/mcp/Mcp get prompt.spec.md`
- New: `specification/commands/ai/mcp/schema/Mcp read resource.schema.yaml`
- New: `specification/commands/ai/mcp/schema/Mcp get prompt.schema.yaml`
- New: `specification/commands/ai/mcp/tests/Mcp client tests.spec.yaml`
- Update: `specification/commands/ai/mcp/Mcp server.spec.md` (rename `Mcp tool call` references)
- Update: `specification/commands/ai/mcp/Mcp tool.spec.md` (rename `Mcp tool call` references)
- Update: `specification/commands/ai/mcp/Stop mcp server.spec.md` (rename reference)
- Update: `specification/commands/README.md`
- Update: `specification/levels.yaml`
- Update: `specification/overview/specscript-overview-agents.md`
- Kotlin: new `McpReadResource.kt`, `McpGetPrompt.kt`, rename `McpToolCall` registration
- TypeScript: new commands in `mcp-server.ts`, rename registration
- Samples: update `call-hello.spec.yaml`, `call-alerts.spec.yaml`
