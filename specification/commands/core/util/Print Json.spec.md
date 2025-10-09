# Command: Print Json

`Print Json` prints contents in JSON representation to the console.

| Content type | Supported |
|--------------|-----------|
| Value        | yes       |
| List         | yes       |
| Object       | yes       |

[Print.schema.yaml](schema/Print.schema.yaml)

## Basic usage

Use **Print Json** to see contents in JSON representation.

```yaml specscript
Code example: Print an object as JSON

Print Json:
  greeting: Hello, World!
```

will print

```json
{
  "greeting": "Hello, World!"
}
```
