# Command: Mcp close session

`Mcp close session` closes an open [Mcp session](Mcp%20session.spec.md) and disconnects from the MCP server. Closing the
current session makes the previously opened session current again.

| Input  | Supported    |
|--------|--------------|
| Value  | yes          |
| List   | auto-iterate |
| Object | yes          |

[Mcp close session.schema.yaml](schema/Mcp%20close%20session.schema.yaml)

## Usage

Pass the session name as a string value to close the session.

```yaml specscript
Code example: Close a session by name

Mcp server:
  name: close-demo
  port: 8099
  tools:
    ping:
      script:
        Output: pong

---
Mcp session:
  name: worker-a
  url: http://localhost:8099/mcp

Mcp call tool:
  tool: ping

Expected output: pong

---
Mcp close session: worker-a
```

You can also pass the session object itself.

```yaml specscript
Code example: Close a session with the session object

Mcp session:
  url: http://localhost:8099/mcp
As: ${session}

Mcp close session: ${session}
```

<!-- yaml specscript
Stop mcp server: close-demo
-->
