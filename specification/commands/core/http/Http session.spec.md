# Command: Http session

`Http session` opens a session that provides default parameters for all subsequent HTTP commands
like [GET](GET.spec.md), [POST](POST.spec.md), etc.

| Input  | Supported    |
|--------|--------------|
| Value  | no           |
| List   | auto-iterate |
| Object | yes          |

[Http session.schema.yaml](schema/Http%20session.schema.yaml)

## Basic usage

Open a session with the HTTP server and credentials, then issue a normal **GET**.

```yaml specscript
Code example: Http session usage

Http session:
  name: backend
  url: http://localhost:2525
  username: admin
  password: admin

GET: /items

Expected output:
  - 1
  - 2
  - 3

Http close session: backend
```

All session parameters are available to subsequent HTTP commands (if applicable): `url`, `path`, `body`, `headers`,
`cookies`, `username`, `password`, `save as`. Parameters given on the request itself override the session parameters.

Sessions are closed automatically when the script ends. Use
[Http close session](Http%20close%20session.spec.md) to close a session earlier.

## Session names

The session name serves as its id. If you don't specify one, it will be generated. The output of **Http session** is the
session object, including the name.

```yaml specscript
Code example: The session object

Http session:
  url: http://localhost:2525
As: ${session}

Assert that:
  - item:
      url: http://localhost:2525
    in: ${session}

Fields: ${session}
Expected output:
  - url
  - name
```

## Multiple sessions

The most recently opened session is the current one, and HTTP commands use it by default. A request without parameters
uses the session parameters as-is.

```yaml specscript
Code example: The current session

Http session:
  name: items
  url: http://localhost:2525
  path: /items

---
Http session:
  name: hello
  url: http://localhost:2525
  path: /hello

GET: { }

Expected output: Hello from SpecScript!
```

Target another open session with the `session` property.

```yaml specscript
Code example: Targeting a session by name

GET:
  session: items

Expected output:
  - 1
  - 2
  - 3
```

Open sessions form a stack: closing the current session makes the previously opened one current again.

```yaml FIXME specscript
Code example: Closing the current session

Http close session: hello

GET: { }

Expected output:
  - 1
  - 2
  - 3
---
Http close session: items
```
