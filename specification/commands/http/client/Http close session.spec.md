# Command: Http close session

`Http close session` closes an open [Http session](Http%20session.spec.md). Closing the current session makes the
previously opened session current again.

| Input  | Supported    |
|--------|--------------|
| Value  | yes          |
| List   | auto-iterate |
| Object | yes          |

[Http close session.schema.yaml](schema/Http%20close%20session.schema.yaml)

## Usage

Pass the session name as a string value to close the session.

```yaml specscript
Code example: Close a session by name

Http session:
  name: backend
  url: http://localhost:2525

GET: /items

Http close session: backend
```

You can also pass the session object itself.

```yaml specscript
Code example: Close a session with the session object

Http session:
  url: http://localhost:2525
As: ${session}

Http close session: ${session}
```
