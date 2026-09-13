# Command: DELETE

`DELETE` sends a DELETE request to an HTTP endpoint.

| Input  | Supported    |
|--------|--------------|
| Value  | no           |
| List   | auto-iterate |
| Object | yes          |

[DELETE.schema.yaml](schema/DELETE.schema.yaml)

## Basic usage

Just specify the endpoint to send the **DELETE** request to.

```yaml specscript
Code example: Simple DELETE

DELETE: http://localhost:2525/items
```

or use the longer form if you need to specify more details

```yaml specscript
Code example: DELETE with more properties

DELETE:
  url: http://localhost:2525/items
  username: admin
  password: admin
```

## Http session

As with all Http commands, you can use [Http session](Http%20session.spec.md) to set the defaults for common fields.

```yaml specscript
Code example: Http session and DELETE

Http session:
  url: http://localhost:2525

DELETE: /items
```

See [Http session](Http%20session.spec.md) for more information on how to configure all fields.
