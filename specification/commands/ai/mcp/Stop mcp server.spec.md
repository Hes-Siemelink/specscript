# Command: Stop mcp server

`Stop mcp server` stops a running MCP server by name.

| Input      | Supported     |
|------------|---------------|
| Scalar     | yes           |
| List       | no            |
| Object     | no            |

[Stop mcp server.schema.yaml](schema/Stop%20mcp%20server.schema.yaml)

## Usage

Pass the server name as a string value to stop and remove the server:

```yaml specscript
Code example: Start and stop MCP server

Mcp server:
  name: stop-demo
  version: "1.0.0"
  port: 8096
  tools:
    ping:
      description: A simple ping tool
      script:
        Output: pong

Mcp tool call:
  tool: ping
  server:
    url: "http://localhost:8096/mcp"

Expected output: pong

Stop mcp server: stop-demo
```

Other servers continue operating; only the named server is stopped.
