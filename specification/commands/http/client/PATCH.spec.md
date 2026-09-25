# Command: PATCH

`PATCH` sends a PATCH request to an HTTP endpoint.

| Input  | Supported    |
|--------|--------------|
| Value  | no           |
| List   | auto-iterate |
| Object | yes          |

[PATCH.schema.yaml](schema/PATCH.schema.yaml)

## Basic usage

Specify `url` and `body` to send a **PATCH** request.

```yaml specscript
Code example: Simple PATCH

PATCH:
  url: http://localhost:2525/items
  body:
    item: one
```

## Http session

As with all Http commands, you can use [Http session](Http%20session.spec.md) to set the defaults for common fields.

```yaml specscript
Code example: Http session and PATCH

Http session:
  url: http://localhost:2525

PATCH:
  path: /items
  body:
    item: one
```

See [Http session](Http%20session.spec.md) for more information on how to configure all fields.
