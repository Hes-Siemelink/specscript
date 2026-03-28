# Command: Base64 decode

Use `Base64 decode` to decode stuff. See [Base64 encode](Base64%20encode.spec.md).

| Input      | Supported     |
|------------|---------------|
| Scalar     | yes           |
| List       | auto-iterate  |
| Object     | no            |

[Base64 decode.schema.yaml](schema/Base64%20decode.schema.yaml)

## Basic usage

Use **Base64 decode** to decode a message

```yaml specscript
Code example: Base 64 decode

Base64 decode: SGVsbG8gd29ybGQh

Expected output: Hello world!
```
