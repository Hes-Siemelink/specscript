# Command: Mcp tool call

`Mcp tool call` executes a tool on an MCP server.

| Input      | Supported     |
|------------|---------------|
| Scalar     | no            |
| List       | auto-iterate  |
| Object     | yes           |
| Raw input  | yes           |

[CallMcpTool.schema.yaml](schema/Call%20Mcp%20tool.schema.yaml)

## Basic usage

Use **Mcp tool call** to execute tools on MCP servers.

Suppose we have this simple MCP server that provides a welcome message through the  `hello` tool:

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

Then we can call the MCP `greet` tool like this:

```yaml specscript
Code example: Basic Mcp tool call

Mcp tool call:
  tool: hello
  server:
    url: "http://localhost:8091/mcp"

Expected output: Hello there!
```

## Passing arguments

You can pass arguments to the tool via the `input` property:

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
    url: "http://localhost:8091/mcp"

Expected output: Hello Alice!
```

Variables are resolved in the `input` block before sending to the server:

```yaml specscript
Code example: Mcp tool call with variable input

${greeting_name}: Bob

Mcp tool call:
  tool: greet
  input:
    name: ${greeting_name}
  server:
    url: "http://localhost:8091/mcp"

Expected output: Hello Bob!
```

## Transport types

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
    transport: STDIO
    command: bash specification/commands/ai/mcp/mock-mcp-server.sh

Expected output: Mock server response
```

### HTTP

HTTP server type enables connection to MCP servers using the Streamable HTTP transport. Supports authentication via
Bearer tokens and custom headers.

```yaml
Code example: HTTP transport

Mcp tool call:
  server:
    transport: HTTP
    url: "https://api.example.com/mcp"
    headers:
      Authorization: "Bearer ${API_TOKEN}"
      Content-Type: "application/json"
  tool: analyze_data
  input:
    data: "sample input"
```

### SSE

SSE server type enables connection to MCP servers using the legacy Server-Sent Events transport.

```yaml
Code example: SSE transport

Mcp tool call:
  server:
    transport: SSE
    url: "http://localhost:8091/mcp"
  tool: hello
```

<!-- yaml specscript
Stop mcp server: demo-server
-->