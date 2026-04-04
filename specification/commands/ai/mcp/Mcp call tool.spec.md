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

```yaml specscript
Code example: Simple MCP server

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

```yaml specscript
Code example: Basic Mcp call tool

Mcp call tool:
  tool: hello
  server:
    url: "http://localhost:8091/mcp"

Expected output: Hello there!
```

## Passing input

Pass arguments to the tool via the `input` property:

```yaml specscript
Code example: Mcp call tool with input

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

<!-- yaml specscript
Stop mcp server: demo-server
-->

## Transport types

The `server` property defines how to connect to the MCP server. Two transport types are supported: `http` (default)
and `stdio`.

### HTTP

HTTP is the default transport. It uses the Streamable HTTP protocol for MCP communication.

```yaml
Code example: HTTP transport

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
Code example: Stdio transport

Mcp call tool:
  tool: any_tool
  server:
    transport: STDIO
    command: node my-mcp-server.js
```

