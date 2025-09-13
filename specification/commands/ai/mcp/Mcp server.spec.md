# Command: Mcp server

`Mcp server` starts an MCP server.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | no        |
| Object       | yes       |

[McpServer.schema.yaml](schema/McpServer.schema.yaml)

## Basic usage

Use **Mcp server** to start a server with tools, resources and prompts.

```yaml specscript
Code example: Simple MCP server

Mcp server:
  name: my-server
  version: "1.0.0"
  tools:
    hello:
      description: Get a greeting
      inputSchema:
        name:
          type: string
          description: Your name
      script:
        Output: Hello ${input.name}!
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
        data:
          type: string
          description: Data to process
      script: process-data.cli
```

The external script file should contain the SpecScript commands to execute when the tool is called.

<!-- TODO: Need to stop the file-server after Mcp tool command is implemented to avoid interference with other tests -->

### Stop the server

Stop and remove the server with the `stop` command:

```yaml specscript
Code example: Stop server

Mcp server:
  name: my-server
  version: "1.0.0"
  stop: true
```
