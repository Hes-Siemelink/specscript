# Command: Mcp read resource

`Mcp read resource` reads a resource from an MCP server.

| Input     | Supported    |
|-----------|--------------|
| Value     | no           |
| List      | auto-iterate |
| Object    | yes          |
| Raw input | yes          |

[Mcp read resource.schema.yaml](schema/Mcp%20read%20resource.schema.yaml)

## Basic usage

Suppose we have an MCP server that provides a `config://app` resource:

```yaml specscript
Code example: MCP server with resource

Mcp server:
  name: resource-demo
  port: 8091
  resources:
    config://app:
      name: App Config
      description: Application configuration
      output:
        theme: dark
        version: "2.0"
```

Read the resource using `Mcp read resource`:

```yaml specscript
Code example: Basic Mcp read resource

Mcp read resource:
  uri: "config://app"
  server:
    url: "http://localhost:8091/mcp"

Expected output:
  theme: dark
  version: "2.0"
```

<!-- yaml specscript
Stop mcp server: resource-demo
-->

## Sessions

By default, `Mcp read resource` opens a new connection for each call. When an [Mcp session](Mcp%20session.spec.md) is
open, `Mcp read resource` uses it and the `server` property can be omitted.

Target a specific open session with the `session` property. (Note that `session` and `server` are mutually exclusive —
give at most one.)

```yaml specscript
Code example: Reading a resource in a session

Mcp server:
  name: resource-session-demo
  port: 8092
  resources:
    config://app:
      name: App Config
      description: Application configuration
      output:
        theme: dark
        version: "2.0"

Mcp session:
  name: demo
  url: http://localhost:8092/mcp

Mcp read resource:
  session: demo
  uri: "config://app"

Expected output:
  theme: dark
  version: "2.0"
```

If you don't specify a session, the most recently opened one is used:

```yaml specscript
Mcp read resource:
  uri: "config://app"

Expected output:
  theme: dark
  version: "2.0"
```

<!-- yaml specscript
Stop mcp server: resource-session-demo
-->
