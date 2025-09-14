# Command: Call mcp tool

`Call mcp tool` executes a tool on an MCP server.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | no        |
| Object       | yes       |

[CallMcpTool.schema.yaml](schema/CallMcpTool.schema.yaml)

## Basic usage

Use **Call mcp tool** to execute tools on MCP servers. The connection details are specified directly on the command.

```yaml specscript
Code example: Start server and call tool via client

Mcp server:
  name: demo-server
  version: "1.0.0"
  tools:
    greet:
      description: Generate a personalized greeting
      inputSchema:
        type: object
        properties:
          name:
            type: string
            description: Name of the person to greet
        required: [name]
        additionalProperties: false
      script:
        Output: Hello ${input.name}!

# Call the tool using MCP client
Call mcp tool:
  server: demo-server
  transport:
    type: internal
  tool: greet
  arguments:
    name: Alice

Expected output: Hello Alice!
```

## External server connection

Connect to external MCP servers by specifying connection details (process transport not implemented in spike):

```yaml
# This will be implemented in future iterations
Call mcp tool:
  server: external-server
  transport:
    type: process
    command: ["python", "my-mcp-server.py"]
  tool: analyze_code
  arguments:
    code: "def hello(): pass"
    language: python
```

## Connection details

The `transport` property defines how to connect to the MCP server:

### Internal transport

For connecting to running servers started by `Mcp server` (Phase 1 implementation):

```yaml specscript
Code example: Internal transport connection

Mcp server:
  name: local-server
  version: "1.0.0"
  tools:
    echo:
      description: Echo the input
      inputSchema:
        type: object
        properties:
          message:
            type: string
        required: [message]
        additionalProperties: false
      script:
        Output: ${input.message}

Call mcp tool:
  server: local-server
  transport:
    type: internal
  tool: echo
  arguments:
    message: "Test message"

Expected output: Test message
```

### Stdio transport

For spawning external MCP server processes via shell commands:

```yaml specscript
Code example: Stdio transport with mock MCP server

Call mcp tool:
  server: mock-stdio-server
  transport:
    type: stdio
    command: "specification/commands/ai/mcp/mock-mcp-server.sh"
  tool: any_tool
  arguments:
    data: "sample input"

Expected output: Mock server response
```

Note: Stdio transport enables connection to external MCP-compliant servers. The example uses a simple mock server - real usage works with `python my-mcp-server.py` or any MCP server process.

### HTTP transport

For connecting to HTTP-based MCP servers:

```yaml
# Example of HTTP transport usage (requires running HTTP MCP server)
Call mcp tool:
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

Note: HTTP transport enables connection to MCP servers over HTTP. Supports authentication via Bearer tokens and custom headers. The server must implement the MCP HTTP protocol.

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
        required: [input]
        additionalProperties: false
      script:
        Error: This tool intentionally fails

Call mcp tool:
  server: error-server
  transport:
    type: internal
  tool: failing_tool
  arguments:
    input: "test"

Expected error: This tool intentionally fails
```

<!-- yaml specscript
Mcp server:
  name: demo-server
  version: "1.0.0"
  stop: true

Mcp server:
  name: local-server
  version: "1.0.0"
  stop: true

Mcp server:
  name: error-server
  version: "1.0.0"
  stop: true
-->