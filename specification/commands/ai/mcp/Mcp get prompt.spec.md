# Command: Mcp get prompt

`Mcp get prompt` gets a prompt from an MCP server.

| Input     | Supported    |
|-----------|--------------|
| Value     | no           |
| List      | auto-iterate |
| Object    | yes          |
| Raw input | yes          |

[Mcp get prompt.schema.yaml](schema/Mcp%20get%20prompt.schema.yaml)

## Basic usage

Suppose we have an MCP server that provides a `greet` prompt:

```yaml specscript
Code example: MCP server with prompt

Mcp server:
  name: prompt-demo
  port: 8091
  prompts:
    greet:
      description: A greeting prompt
      arguments:
        - name: name
          description: Who to greet
          required: true
      script:
        Output: Hello ${input.name}, how can I help you today?
```

Get the prompt using `Mcp get prompt`:

```yaml specscript
Code example: Basic Mcp get prompt

Mcp get prompt:
  prompt: greet
  input:
    name: Alice
  server:
    url: "http://localhost:8091/mcp"

Expected output: Hello Alice, how can I help you today?
```

<!-- yaml specscript
Stop mcp server: prompt-demo
-->
