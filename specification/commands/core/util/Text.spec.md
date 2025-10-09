# Command: Text

Use `Text` to convert structured data into a string.

| Content type | Supported |
|--------------|-----------|
| Value        | yes       |
| List         | yes       |
| Object       | yes       |

## Basic usage

Use **To text** when you need a string representation of your data.

```yaml specscript
Code example: Yaml to text

Text:
  message: Hello world!

Expected output: "message: Hello world!"
```

Note: there is no guarantee on the exact formatting of the output.

See **[Json](Json.spec.md)** for a more compact and canonical format.