# Command: POST

`POST` sends a POST request to an HTTP endpoint.

| Input  | Supported    |
|--------|--------------|
| Value  | no           |
| List   | auto-iterate |
| Object | yes          |

[POST.schema.yaml](schema/POST.schema.yaml)

## Basic usage

Specify `url` and `body` to send a **POST** request.

```yaml specscript
Code example: Simple POST

POST:
  url: http://localhost:2525/items
  body:
    1: One
    2: Two
    3: Three
```

## Post without body

You can use the shortcut notation to send a POST request without a body.

```yaml specscript
Code example: POST without body

POST: http://localhost:2525/echo/body

Expected output: { }
```

## Http session

As with all Http commands, you can use [Http session](Http%20session.spec.md) to set the defaults for common fields.

```yaml specscript
Code example: Http session and POST

Http session:
  url: http://localhost:2525

POST:
  path: /items
  body:
    1: One
    2: Two
    3: Three
```

See [Http session](Http%20session.spec.md) for more information on how to configure all fields.
