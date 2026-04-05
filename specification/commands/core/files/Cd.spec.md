# Command: Cd

`Cd` changes the working directory for all subsequent commands.

| Input  | Supported |
|--------|-----------|
| Value  | yes       |
| List   | no        |
| Object | no        |

## Basic usage

Commands like `Read file`, `Run`, and `Shell` resolve file paths relative to the working directory. Use `Cd` to change
it.

Given a file `data.yaml` in the temp directory:

```yaml specscript
Temp file:
  name: data.yaml
  content:
    greeting: Hello
```

Change to the temp directory and read the file by name:

```yaml specscript
Code example: Cd to temp directory

Cd: ${SCRIPT_TEMP_DIR}

Read file: data.yaml

Expected output:
  greeting: Hello
```

## Persists for subsequent commands

The working directory change persists for all commands that follow in the same script scope.

```yaml specscript
Code example: Cd persists

# Previous example has already set Cd: ${SCRIPT_TEMP_DIR}

Shell: ls

Expected output: data.yaml
```
