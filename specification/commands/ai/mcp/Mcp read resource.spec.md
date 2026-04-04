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
