# Command: Mcp tool

`Mcp tool` defines tools for an MCP server.

| Input      | Supported     |
|------------|---------------|
| Scalar     | no            |
| List       | auto-iterate  |
| Object     | yes           |
| Raw input  | yes           |

[Mcp tool.schema.yaml](schema/Mcp%20tool.schema.yaml)

## Basic usage

Use **Mcp tool** to define tools that can be added to an MCP server. This command is used in conjunction with
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
    script: process-data.spec.yaml
```

The external script file should contain the SpecScript commands to execute when the tool is called.

## Static output

Use `output` to return a fixed value, useful for mock servers and testing:

```yaml specscript
Code example: Tool with static output

Mcp tool:
  version:
    description: Returns the API version
    output: "2.0.0"
```

When both `output` and `script` are present, `output` takes precedence.

<!-- yaml specscript
Stop mcp server: test-server
-->

## Deriving metadata from script

When a tool references an external script file, both `description` and `inputSchema` can be omitted. SpecScript derives
them from the script's `Script info` and `Input schema` commands.

Given a script file `greet-tool.spec.yaml`:

```yaml temp-file=greet-tool.spec.yaml
Script info: Generate a personalized greeting

Input schema:
  type: object
  properties:
    name:
      description: Name of the person to greet
      default: World

Output: Hello, ${input.name}!
```

The tool definition only needs to reference the script file:

```yaml specscript
Code example: MCP tool with derived metadata

Mcp server:
  name: derive-server
  version: "1.0.0"
  port: 8094
```

```yaml specscript
Code example: Tool with metadata derived from script

Mcp tool:
  greet:
    script: greet-tool.spec.yaml
```

Calling the tool works as expected:

```yaml specscript
Code example: Calling a tool with derived metadata

Mcp call tool:
  tool: greet
  input:
    name: Alice
  server:
    url: "http://localhost:8094/mcp"

Expected output: Hello, Alice!
```

<!-- yaml specscript
Stop mcp server: derive-server
-->

Explicit `description` or `inputSchema` on the tool definition takes precedence over what the script provides.