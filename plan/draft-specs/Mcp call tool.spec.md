> **Draft.** Describes proposed behavior for `Mcp call tool` that doesn't exist yet — the `Sessions` section below.
> Everything above it already works and is copied from the real spec at
> `specification/commands/ai/mcp/Mcp call tool.spec.md`. Rationale lives in
> [plan/proposals/mcp-client-state.md](../proposals/mcp-client-state.md). Code blocks here are illustrative only, not
> executed by `specificationTest`.

# Command: Mcp call tool

`Mcp call tool` executes a tool on an MCP server.

| Input     | Supported    |
|-----------|--------------|
| Value     | no           |
| List      | auto-iterate |
| Object    | yes          |
| Raw input | yes          |

[Mcp call tool.schema.yaml](schema/Mcp%20call%20tool.schema.yaml)

## Basic usage

Suppose we have this MCP server that provides a `hello` tool:

```yaml
Mcp server:
  name: demo-server
  port: 8091
  tools:
    hello:
      description: Generate a personalized greeting
      script:
        Output: Hello there!
```

Call the tool using `Mcp call tool`:

```yaml
Mcp call tool:
  tool: hello
  server:
    url: "http://localhost:8091/mcp"

Expected output: Hello there!
```

## Passing input

Pass arguments to the tool via the `input` property:

```yaml
Mcp tool:
  greet:
    description: Generate a personalized greeting
    inputSchema:
      type: object
      properties:
        name:
          type: string
          description: Name of the person to greet
    script:
      Output: Hello ${input.name}!


Mcp call tool:
  tool: greet
  input:
    name: Alice
  server:
    url: "http://localhost:8091/mcp"

Expected output: Hello Alice!
```

## Transport types

The `server` property defines how to connect to the MCP server. Two transport types are supported: `http` (default)
and `stdio`.

### HTTP

HTTP is the default transport. It uses the Streamable HTTP protocol for MCP communication.

```yaml
Mcp call tool:
  server:
    transport: HTTP
    url: "https://api.example.com/mcp"
    headers:
      Authorization: "Bearer ${API_TOKEN}"
  tool: analyze_data
  input:
    data: "sample input"
```

### Stdio

Stdio enables connection to external MCP-compliant servers over standard input/output streams.

```yaml
Mcp call tool:
  tool: any_tool
  server:
    transport: STDIO
    command: node my-mcp-server.js
```

## Sessions (proposed)

By default, `Mcp call tool` connects, sends the request, and disconnects — every call is independent, and a server
that keeps connection-scoped state (MCP's Streamable HTTP transport has an explicit session for exactly this) sees a
brand-new, stateless connection every time.

`Mcp call tool` reuses a connection automatically when the same `server` is targeted again in the same script run —
no extra syntax needed:

```yaml
Mcp call tool:
  tool: remember
  input:
    value: 42
  server:
    url: "http://localhost:8091/mcp"

Mcp call tool:
  tool: recall
  server:
    url: "http://localhost:8091/mcp"

Expected output: 42
```

Reuse is keyed by the full connection identity — url, transport, headers, and token together, not url alone, since
two calls with different credentials must never end up sharing a connection. A script that doesn't care about this
doesn't need to change anything; it only matters for a server that actually keeps state between calls.

### Explicit sessions

For an explicit, named session — two independent connections to the same server, or a session whose lifetime you
want visible and deliberate rather than incidental — use `Mcp start session` and `Mcp stop session`:

```yaml
Mcp start session:
  name: worker-a
  server:
    url: "http://localhost:8091/mcp"

Mcp call tool:
  session: worker-a
  tool: remember
  input:
    value: 42

Mcp call tool:
  session: worker-a
  tool: recall

Mcp stop session: worker-a

Expected output: 42
```

`session` and `server` are mutually exclusive on `Mcp call tool` — give exactly one.

(`Mcp start session` and `Mcp stop session` would likely get their own spec files once implemented, the way
`Mcp server` and `Stop mcp server` already do — kept together with `Mcp call tool` here since this one draft covers
the whole related change.)

### Session lifetime

An automatic (unnamed) session lives for the duration of the running script — including any script it calls via
`Run`, inline or by file — and closes when the script finishes. Nothing to clean up manually. A named session from
`Mcp start session` lives until an explicit `Mcp stop session`, or until the running script finishes, whichever comes
first.
