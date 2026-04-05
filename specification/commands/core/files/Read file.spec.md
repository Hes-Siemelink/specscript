# Command: Read file

`Read file` loads Yaml from a file

| Input  | Supported |
|--------|-----------|
| Value  | yes       |
| List   | no        |
| Object | no        |

[Read file.schema.yaml](schema/Read%20file.schema.yaml)

## Basic usage

Use **Read file** to load a Yaml / Json file. The `resource` parameter resolves the file relative to the script's
directory:

```yaml temp-file=greeting.yaml
greeting: Hello
language: en
```

```yaml specscript
Code example: Read Yaml from a file

Read file:
  resource: greeting.yaml

Expected output:
  greeting: Hello
  language: en
```

You can also use `${SCRIPT_HOME}` to build an explicit path:

```yaml specscript
Code example: Read a local file

Read file: ${SCRIPT_HOME}/greeting.yaml

Expected output:
  greeting: Hello
  language: en    
```

## Reading temp files created in Markdown

For files created with ````yaml temp-file=filename.ext` in the same markdown document, use the `resource` parameter.
Suppose we have a file `config.yaml`:

```yaml temp-file=config.yaml
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
