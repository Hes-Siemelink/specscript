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
  name: greet
  arguments:
    name: Alice
  server:
    url: "http://localhost:8091/mcp"

Expected output: Hello Alice, how can I help you today?
```

<!-- yaml specscript
Stop mcp server: prompt-demo
-->

## Sessions

By default, `Mcp get prompt` opens a new connection for each call. When an [Mcp session](Mcp%20session.spec.md) is
open, `Mcp get prompt` uses it and the `server` property can be omitted.

Target a specific open session with the `session` property. (Note that `session` and `server` are mutually exclusive —
give at most one.)

```yaml specscript
Code example: Getting a prompt in a session

Mcp server:
  name: prompt-session-demo
  port: 8093
  prompts:
    greet:
      description: A greeting prompt
      arguments:
        - name: name
          description: Who to greet
          required: true
      script:
        Output: Hello ${input.name}, how can I help you today?

Mcp session:
  name: demo
  url: http://localhost:8093/mcp

Mcp get prompt:
  session: demo
  name: greet
  arguments:
    name: Bob

Expected output: Hello Bob, how can I help you today?
```

If you don't specify a session, the most recently opened one is used:

```yaml specscript
Mcp get prompt:
  name: greet
  arguments:
    name: Carol

Expected output: Hello Carol, how can I help you today?
```

<!-- yaml specscript
Stop mcp server: prompt-session-demo
-->
