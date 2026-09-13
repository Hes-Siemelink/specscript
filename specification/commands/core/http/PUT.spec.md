# Command: PUT

`PUT` sends a PUT request to an HTTP endpoint.

| Input  | Supported    |
|--------|--------------|
| Value  | no           |
| List   | auto-iterate |
| Object | yes          |

[PUT.schema.yaml](schema/PUT.schema.yaml)

## Basic usage

Specify `url` and `body` to send a **PUT** request.

```yaml specscript
Code example: Simple PUT

PUT:
  url: http://localhost:2525/items
  body:
    1: One
    2: Two
    3: Three
```

## Http session

As with all Http commands, you can use [Http session](Http%20session.spec.md) to set the defaults for common fields.

```yaml specscript
Code example: Http session and PUT

Http session:
  url: http://localhost:2525

PUT:
  path: /items
  body:
    1: One
    2: Two
    3: Three
```

See [Http session](Http%20session.spec.md) for more information on how to configure all fields.
