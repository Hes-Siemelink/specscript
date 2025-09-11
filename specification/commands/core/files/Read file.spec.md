# Command: Read file

`Read file` loads Yaml from a file

| Content type | Supported |
|--------------|-----------|
| Value        | yes       |
| List         | no        |
| Object       | no        |

[Read file.schema.yaml](schema/Read%20file.schema.yaml)

## Basic usage

Use **Read file** to load a Yaml / Json file

For example, we have a file `greeting.yaml` in the `specification/commands/core/files` directory:

```yaml file=greeting.yaml
greeting: Hello
language: en
```

Then you can read it like this:

```yaml specscript
Code example: Read Yaml from a file

Read file: specification/commands/core/files/greeting.yaml

Expected output:
  greeting: Hello
  language: en
```

## Reading a file in the same directory as your script

If you want to load a file that is next to your script, use the `resource` parameter:

```yaml specscript
Code example: Read a local file

Read file:
  resource: greeting.yaml

Expected output:
  greeting: Hello
  language: en    
```

## Reading temp files created in Markdown

For files created with ````yaml file=filename.ext` in the same markdown document, use the `resource` parameter:

```yaml file=config.yaml
database:
  host: localhost
  port: 5432
```

```yaml specscript
Code example: Read temp file from markdown

Read file:
  resource: config.yaml

Expected output:
  database:
    host: localhost
    port: 5432
```