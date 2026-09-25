# Command: Mcp session

`Mcp session` opens a connection to an MCP server that stays alive across calls. Subsequent commands
like [Mcp call tool](Mcp%20call%20tool.spec.md) use the session by default, so they don't need the `server` property.

The session maintains the MCP session id, so servers that keep state on the session work correctly across calls.

| Input  | Supported    |
|--------|--------------|
| Value  | no           |
| List   | auto-iterate |
| Object | yes          |

[Mcp session.schema.yaml](schema/Mcp%20session.schema.yaml)

## Basic usage

Suppose we have this MCP server with some tools on it:

```yaml specscript
Code example: Simple MCP server

Mcp server:
  name: session-demo
  port: 8097
  tools:
    hello:
      description: A simple greeting
      script:
        Output: Hello there!
    bye:
      description: A simple farewell
      script:
        Output: Bye now!
```

When doing multiple calls, you can open a session with **Mcp session** and do several tools calls, without having to
specify the server each time.

```yaml specscript
Code example: Mcp session usage

Mcp session:
  name: session-1
  url: http://localhost:8097/mcp

Mcp call tool:
  name: hello

---
Mcp call tool:
  name: bye

Expected output: Bye now!

---
Mcp close session: session-1
```

Sessions are closed automatically when the script ends. Use
[Mcp close session](Mcp%20close%20session.spec.md) to close a session earlier.

The most recently opened session is the current one. Open sessions form a stack: closing the current session makes the
previously opened one current again. Commands can target another open session with the `session` property —
see [Mcp call tool](Mcp%20call%20tool.spec.md).

## Session names

The session name serves as its id. If you don't specify one, it will be generated. The output of **Mcp session** is the
session object, including the name.

```yaml specscript
Code example: The session object

Mcp session:
  url: http://localhost:8097/mcp
As: ${session}

Assert that:
  item:
    url: http://localhost:8097/mcp
  in: ${session}

Mcp close session: ${session}
```

<!-- yaml specscript
Stop mcp server: session-demo
-->

## Transport types

`Mcp session` takes the same connection properties as the `server` property
of [Mcp call tool](Mcp%20call%20tool.spec.md): `transport` (`HTTP` or `STDIO`), `url`, `command`, `headers`
and `token`.

For example, a session to a stdio server that keeps state on the connection:

```yaml
Code example: Stdio session

Mcp session:
  name: browser
  transport: STDIO
  command: npx @playwright/mcp@latest
```
