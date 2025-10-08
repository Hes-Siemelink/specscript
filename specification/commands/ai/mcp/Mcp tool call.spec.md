# Command: Mcp tool call

`Mcp tool call` executes a tool on an MCP server.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | implicit  |
| Object       | yes       |

[CallMcpTool.schema.yaml](schema/Call%20Mcp%20tool.schema.yaml)

## Basic usage

Use **Mcp tool call** to execute tools on MCP servers.

Suppose we have this simple MCP server that provides a welcome message through the  `hello` tool:

```yaml specscript
Code example: Simple MCP server

Mcp server:
  name: demo-server
  version: "1.0.0"
  transport: SSE
  port: 8091
  tools:
    hello:
      description: Generate a personalized greeting
      script:
        Output: Hello there!
```

Then we can call the MCP `greet` tool like this:

```yaml specscript
Code example: Basic Mcp tool call

Mcp tool call:
  tool: hello
  server:
    type: sse
    url: "http://localhost:8091"

Expected output: Hello there!
```

Here we use the `internal` transport to connect to the `demo-server` MCP server that we defined earlier. Other supported
transport types are `stdio` and `http` (see below).

## Passing arguments

You can pass arguments to the tool via the `arguments` property:

```yaml specscript
Code example: Mcp tool with arguments

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


Mcp tool call:
  tool: greet
  input:
    name: Alice
  server:
    type: sse
    url: "http://localhost:8091"

Expected output: Hello Alice!

```

## Server types

The `server` property defines how to connect to the MCP server. There are three supported transport types:
`stdio`, `http` and `sse`.

### Stdio

Stdio enables connection to external MCP-compliant servers over standard input/output streams. This is useful for
connecting to MCP servers implemented in any programming language that can run on your local machine.

```yaml FIXME specscript  ==> Example hangs after running HTTP client before it
Code example: Stdio transport with external process

Mcp tool call:
  tool: any_tool
  input:
    data: sample input
  server:
    type: stdio
    command: bash specification/commands/ai/mcp/mock-mcp-server.sh

Expected output: Mock server response
```

### HTTP

HTTP server type enables connection to MCP servers over HTTP. Supports authentication via Bearer tokens and custom
headers.

```yaml
Code example: HTTP transport

Mcp tool call:
  transport:
    type: http
    url: "https://api.example.com/mcp"
    headers:
      Authorization: "Bearer ${API_TOKEN}"
      Content-Type: "application/json"
  tool: analyze_data
  arguments:
    data: "sample input"
```

<!-- yaml specscript
Mcp server:
  name: demo-server
  version: "1.0.0"
  stop: true
-->