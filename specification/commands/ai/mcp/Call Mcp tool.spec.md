# Command: Call Mcp tool

`Call Mcp tool` executes a tool on an MCP server.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | implicit  |
| Object       | yes       |

[CallMcpTool.schema.yaml](schema/Call%20Mcp%20tool.schema.yaml)

## Basic usage

Use **Call Mcp tool** to execute tools on MCP servers.

Suppose we have this simple MCP server that provides a welcome message through the  `hello` tool:

```yaml specscript
Code example: Simple MCP server

Mcp server:
  name: demo-server
  version: "1.0.0"
  tools:
    hello:
      description: Generate a personalized greeting
      script:
        Output: Hello there!
```

Then we can call the MCP `greet` tool like this:

```yaml specscript
Code example: Basic Mcp tool call

Call Mcp tool:
  tool: hello
  transport:
    type: internal
    server: demo-server

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


Call Mcp tool:
  tool: greet
  arguments:
    name: Alice
  transport:
    type: internal
    server: demo-server

Expected output: Hello Alice!

```

## Transport types

The `transport` property defines how to connect to the MCP server. There are three supported transport types:
`internal`, `stdio`, and `http`.

### Internal transport

When testing out an MCP server, it is convenient to use the `internal` transport type. This allows connecting to MCP
servers that are defined in the same SpecScript session.

Set `type: internal` and put the server name in the `server` property.

```yaml specscript
Code example: Internal transport connection

Call Mcp tool:
  tool: hello
  transport:
    type: internal
    server: demo-server

Expected output: Hello there!
```

### Stdio transport

Stdio transport enables connection to external MCP-compliant servers over standard input/output streams. This is useful
for connecting to MCP servers implemented in any programming language that can run on your local machine.

```yaml specscript
Code example: Stdio transport with external process

Call Mcp tool:
  tool: any_tool
  arguments:
    data: sample input
  transport:
    type: stdio
    command: bash specification/commands/ai/mcp/mock-mcp-server.sh

Expected output: Mock server response
```

### HTTP transport

HTTP transport enables connection to MCP servers over HTTP. Supports authentication via Bearer tokens and custom
headers.

```yaml
# Example of HTTP transport usage (requires running HTTP MCP server)
Call Mcp tool:
  server: http-server
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

## Error handling

Tool execution errors are properly reported:

```yaml specscript
Code example: Tool error handling

Mcp server:
  name: error-server
  version: "1.0.0"
  tools:
    failing_tool:
      description: A tool that always fails
      inputSchema:
        type: object
        properties:
          input:
            type: string
        required: [ input ]
      script:
        Error: This tool intentionally fails

Call Mcp tool:
  tool: failing_tool
  arguments:
    input: "test"
  transport:
    type: internal
    server: error-server

Expected error: This tool intentionally fails
```

<!-- yaml specscript
Mcp server:
  name: demo-server
  version: "1.0.0"
  stop: true

Mcp server:
  name: error-server
  version: "1.0.0"
  stop: true
-->