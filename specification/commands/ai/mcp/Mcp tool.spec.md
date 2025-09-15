# Command: Mcp tool

`Mcp tool` defines tools for an MCP server.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | implicit  |
| Object       | yes       |

[Mcp tool.schema.yaml](schema/Mcp%20tool.schema.yaml)

## Basic usage

Use **Mcp tool** to define tools that can be added to an MCP server. This command is typically used in conjunction with
`Mcp server` to modularize tool definitions.

```yaml specscript
Code example: MCP server without definitions

Mcp server:
  name: test-server
  version: "1.0.0"
```

Now that we have a server running, we can add tools to it.

```yaml specscript
Code example: Adding a tool to an MCP server

Mcp tool:
  greet:
    description: Generate a personalized greeting
    inputSchema:
      type: object
      properties:
        name:
          type: string
          description: Name of the person to greet
      required: [ name ]
      additionalProperties: false
    script:
      Output: Hello ${input.name}!
```

### External script files

You can reference external SpecScript files in the `script` property by providing a filename:

```yaml specscript
Code example: Tool backed by external script

Mcp tool:
  process:
    description: Process some data
    inputSchema:
      type: object
      properties:
        data:
          type: string
          description: Data to process
      required: [ data ]
      additionalProperties: false
    script: process-data.cli
```

The external script file should contain the SpecScript commands to execute when the tool is called.

<!-- yaml specscript
Mcp server:
  name: test-server
  version: "1.0.0"
  stop: true
-->