# Command: Parse Yaml

Use `Parse Yaml` to convert a JSON or YAML string into the structured data format.

| Content type | Supported |
|--------------|-----------|
| Value        | yes       |
| List         | implicit  |
| Object       | no        |

[ Parse yaml.schema.yaml ](schema/Parse%20Yaml.schema.yaml)

## Basic usage

Use **Parse Yaml** when all you got was a string but you need to process it.

```yaml specscript
Code example: Parse yaml example

Parse Yaml: "message: Hello world!"

Expected output:
  message: Hello world!
```

## JSON example

JSON also works, because JSON is a subset of YAML.

```yaml specscript
Code example: Parse json example

Parse Yaml: '{"message": "Hello world!"}'

Expected output:
  message: Hello world!
```

## No YAML or JSON

If the input is not valid YAML or JSON, the input stays as is

```yaml specscript
Code example: No yaml or json

Parse Yaml: 'Invalid::input'

Expected output: 'Invalid::input'
```