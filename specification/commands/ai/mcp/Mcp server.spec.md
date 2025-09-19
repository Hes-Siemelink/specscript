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
    farewell:
      name: Farewell Prompt
      description: A prompt for saying goodbye
      script:
        Output: Goodbye! Have a great day!
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
      script: process-data.cli
```

The external script file should contain the SpecScript commands to execute when the tool is called.

<!-- TODO: Need to stop the file-server after Mcp tool command is implemented to avoid interference with other tests -->

## HTTP transport

Start an HTTP-based MCP server using Server-Sent Events (SSE) for bidirectional communication:

```yaml specscript
Code example: HTTP MCP server

Mcp server:
  name: http-server
  version: "1.0.0"
  transport: HTTP
  port: 8090
  path: "/mcp"
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
```

The HTTP server supports:

- **transport**: Set to `HTTP` to enable HTTP transport (default: `STDIO`)
- **port**: Port number for the HTTP server (default: `8080`)
- **path**: Base path for MCP endpoints (default: `/`)

HTTP clients can connect to `http://localhost:8090/mcp` for SSE and POST messages.

### Stop the server

Stop and remove the server with the `stop` command:

```yaml specscript
Code example: Stop server

Mcp server:
  name: my-server
  version: "1.0.0"
  stop: true
```

<!-- yaml specscript
Mcp server:
  name: file-server
  version: "1.0.0"
  stop: true

Mcp server:
  name: http-server
  version: "1.0.0"
  stop: true
-->
