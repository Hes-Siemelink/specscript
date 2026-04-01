# Command: Read file

`Read file` loads Yaml from a file

| Input      | Supported     |
|------------|---------------|
| Scalar     | yes           |
| List       | no            |
| Object     | no            |

[Read file.schema.yaml](schema/Read%20file.schema.yaml)

## Basic usage

Use **Read file** to load a Yaml / Json file

For example, we have a file `greeting.yaml` in the `specification/commands/core/files` directory:

```yaml temp-file=greeting.yaml
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

Use `${SCRIPT_HOME}` to read a file next to your script:

```yaml specscript
Code example: Read a local file

Read file: ${SCRIPT_HOME}/greeting.yaml

Expected output:
  greeting: Hello
  language: en    
```

Alternatively, you can use the `resource` parameter:

```yaml specscript
Code example: Read a local file with resource

Read file:
  resource: greeting.yaml

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