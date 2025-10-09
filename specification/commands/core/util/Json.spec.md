# Command: Json

Use `Json` to convert structured data into a JSON string.

| Content type | Supported |
|--------------|-----------|
| Value        | yes       |
| List         | yes       |
| Object       | yes       |

## Basic usage

Use **Json** when you need a compact and canonical string representation of your data in JSON format.

```yaml specscript
Code example: Create compact JSON

Json:
  message: Hello world!

Expected output: '{"message":"Hello world!"}'
```

See also **[Text](Text.spec.md)** for a more human-readable format.
