# Command: Mcp server

`Mcp server` starts an MCP server.

| Input     | Supported |
|-----------|-----------|
| Scalar    | no        |
| List      | no        |
| Object    | yes       |
| Raw input | yes       |

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

### Static output

Use `output` instead of `script` to return a fixed value. This is useful for mock servers where you want predictable
responses without writing a script.

```yaml specscript
Code example: MCP server tool with output

Mcp server:
  name: mock-server
  version: "1.0.0"
  port: 8081
  tools:
    get_status:
      description: Returns the system status
      output:
        status: ok
        uptime: 99.9

Mcp call tool:
  tool: get_status
  server:
    url: "http://localhost:8081/mcp"

Expected output:
  status: ok
  uptime: 99.9

Stop mcp server: mock-server
```

The `output` property works on tools, resources, and prompts. When both `output` and `script` are present, `output`
takes precedence.

Call a tool using the `Mcp call tool` command:

```yaml specscript
Code example: Call MCP server tool

Mcp call tool:
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

### Deriving metadata from script

When a tool references an external script, both `description` and `inputSchema` can be omitted. SpecScript derives them
from the script's `Script info` and `Input schema` commands.

Given a script file `say-hello.spec.yaml`:

```yaml temp-file=say-hello.spec.yaml
Script info: Say hello to someone

Input schema:
  type: object
  properties:
    name:
      description: Who to greet
      default: World

Output: Hello, ${input.name}!
```

The MCP server tool definition does not need to repeat the metadata:

```yaml specscript
Code example: MCP server with derived metadata

Mcp server:
  name: derive-demo
  version: "1.0.0"
  port: 8082
  tools:
    say_hello:
      script: say-hello.spec.yaml

Mcp call tool:
  tool: say_hello
  input:
    name: Bob
  server:
    url: "http://localhost:8082/mcp"

Expected output: Hello, Bob!

Stop mcp server: derive-demo
```

Explicit `description` or `inputSchema` on the tool definition takes precedence over what the script provides.

### Tools as a list of scripts

When `tools` is a list of filenames, each file becomes a tool. The tool name is the filename without extensions.
Description and input schema are derived from the script's `Script info` and `Input schema` commands.

Given these three script files:

```yaml temp-file=tool1.spec.yaml
Output: Hello from tool1
```

```yaml temp-file=tool2.spec.yaml
Output: Hello from tool2
```

```yaml temp-file=tool3.spec.yaml
Output: Hello from tool3
```

The server exposes each as a tool:

```yaml specscript
Code example: MCP server with tools as script list

Mcp server:
  name: multi-script-server
  version: "1.0.0"
  port: 8083
  tools:
    - tool1.spec.yaml
    - tool2.spec.yaml
    - tool3.spec.yaml

Mcp call tool:
  tool: tool1
  server:
    url: "http://localhost:8083/mcp"

Expected output: Hello from tool1

Stop mcp server: multi-script-server
```

## Transports

### Streaming HTTP transport

The default transport for MCP servers is the Streamable HTTP transport, which is the recommended transport for MCP
communication.

You can set it explicitly by using `transport: HTTP` in the server definition.

```yaml specscript
Code example: Streaming HTTP MCP server

Mcp server:
  name: http-server
  version: "1.0.0"
  transport: HTTP
  port: 8084
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

Mcp call tool:
  tool: greet
  input:
    name: Bob
  server:
    url: "http://localhost:8084/mcp"

Expected output: Hello Bob via HTTP!

Stop mcp server: http-server
```

### STDIO transport

The `stdio` transport allows communication with the MCP server via standard input and output streams. This is useful for
integrating with external processes that can read/write to stdio.

<!-- TODO: Add example of stdio transport -->