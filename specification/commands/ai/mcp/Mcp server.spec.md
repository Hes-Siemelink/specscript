# Command: Mcp server

`Mcp server` starts an MCP server.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | no        |
| Object       | yes       |

[Mcp server.schema.yaml](schema/Mcp%20server.schema.yaml)

## Basic usage

Use **Mcp server** to start a server with tools, resources and prompts. Define the behavior of each item in SpecScript
using the `script` property.

```yaml specscript
Code example: Simple MCP server

Mcp server:
  name: my-server
  version: "1.0.0"
  tools:
    hello:
      description: Get a greeting
      script:
        Output: Hello world!
  resources:
    welcome-message:
      name: Welcome Message
      description: A friendly welcome message
      script:
        Output: Welcome to the MCP server!
  prompts:
    Farewell Prompt:
      description: A prompt for saying goodbye
      script:
        Output: Goodbye! Have a great day!
```

This will start a server with HTTP transport on `http://localhost:8080/mcp`.

Call a tool using the `Mcp tool call` command:

```yaml specscript
Code example: Call MCP server tool

Mcp tool call:
  tool: hello
  server:
    url: "http://localhost:8080/mcp"

Expected output: Hello world!
```

Stop and remove the server with the `Stop mcp server` command:

```yaml specscript
Code example: Stop server

Stop mcp server: my-server
```

### External script files

You can reference external SpecScript files in the `script` property by providing a filename:

```yaml specscript
Code example: MCP server with external script

Mcp server:
  name: file-server
  version: "1.0.0"
  tools:
    process_data:
      description: Process some data
      inputSchema:
        properties:
          data:
            type: string
            description: Data to process
      script: process-data.spec.yaml
```

The external script file should contain the SpecScript commands to execute when the tool is called.

<!-- yaml specscript
Stop mcp server: file-server
-->

### Deriving input schema from script

When a tool references an external script that uses `Input schema`, the `inputSchema` can be omitted. SpecScript
automatically derives it from the script file.

Given a script file `say-hello.spec.yaml`:

```yaml file=say-hello.spec.yaml
Input schema:
  type: object
  properties:
    name:
      description: Who to greet
      default: World

Output: Hello, ${input.name}!
```

The MCP server tool definition does not need to repeat the schema:

```yaml specscript
Code example: MCP server with derived input schema

Mcp server:
  name: derive-demo
  version: "1.0.0"
  port: 8095
  tools:
    say_hello:
      description: Say hello to someone
      script: say-hello.spec.yaml

Mcp tool call:
  tool: say_hello
  input:
    name: Bob
  server:
    url: "http://localhost:8095/mcp"

Expected output: Hello, Bob!

Stop mcp server: derive-demo
```

If `inputSchema` is provided explicitly, it takes precedence over the script's `Input schema`.

## Transports

### Streaming HTTP transport

The default transport for MCP servers is the Streamable HTTP transport, which is the recommended transport for MCP
communication.

You can set it explicity by using `transport: HTTP` in the server definition.

```yaml specscript
Code example: Streaming HTTP MCP server

Mcp server:
  name: http-server
  version: "1.0.0"
  transport: HTTP
  port: 8092
  tools:
    greet:
      description: Generate a greeting over HTTP
      inputSchema:
        properties:
          name:
            type: string
            description: Name to greet
      script:
        Output: Hello ${input.name} via HTTP!

Mcp tool call:
  tool: greet
  input:
    name: Bob
  server:
    url: "http://localhost:8092/mcp"

Expected output: Hello Bob via HTTP!

Stop mcp server: http-server
```

The streaming HTTP transport supports:

- **transport**: Set to `HTTP` to enable streaming HTTP transport
- **port**: Port number for the HTTP server (default: `8080`)
- Bidirectional communication via POST (requests) and GET (server-initiated SSE notifications)
- Session management via `Mcp-Session-Id` header
- Both JSON and SSE response formats

HTTP clients connect to `http://localhost:<port>/mcp` using the `HTTP` server type.

### SSE transport

The legacy SSE protocol for MCP communication is supported by setting `transport: SSE` in the server definition.

```yaml specscript
Code example: SSE MCP server

Mcp server:
  name: sse-server
  version: "1.0.0"
  transport: SSE
  port: 8090
  tools:
    greet:
      description: Generate a greeting over SSE
      inputSchema:
        properties:
          name:
            type: string
            description: Name to greet
      script:
        Output: Hello ${input.name} via SSE!

Mcp tool call:
  tool: greet
  input:
    name: Alice
  server:
    transport: SSE
    url: http://localhost:8090

Expected output: Hello Alice via SSE!

Stop mcp server: sse-server
```

### STDIO transport

The `stdio` transport allows communication with the MCP server via standard input and output streams. This is useful for
integrating with external processes that can read/write to stdio.

<!-- TODO: Add example of stdio transport -->