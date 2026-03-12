# SCRIPT_DIR Variable

The `${SCRIPT_DIR}` variable contains the absolute path to the directory of the currently executing script. It is
available in all contexts, not just Shell commands.

This is useful for referencing files relative to the script location, making scripts portable regardless of the
working directory.

## Basic usage

The `${SCRIPT_DIR}` variable is always available and points to the directory containing the current script.

```yaml specscript
Code example: SCRIPT_DIR is available

Assert that:
  not:
    empty: ${SCRIPT_DIR}
```

## Using SCRIPT_DIR for file paths

Use `${SCRIPT_DIR}` to reference files next to your script. This makes file paths work regardless of which directory
you run `spec` from.

```yaml file=local-data.json
{ "message": "hello" }
```

```yaml specscript
Code example: Read a file relative to the script

Read file: ${SCRIPT_DIR}/local-data.json

Expected output:
  message: hello
```

## Comparison with Shell

In Shell commands, `SCRIPT_DIR` is available as an environment variable `$SCRIPT_DIR`. In the rest of SpecScript, use
the `${SCRIPT_DIR}` variable syntax.

```yaml specscript
Code example: SCRIPT_DIR in Shell vs SpecScript

Shell: echo $SCRIPT_DIR
As: ${shell_dir}

Assert equals:
  actual: ${shell_dir}
  expected: ${SCRIPT_DIR}
```

## Portable connection scripts

`${SCRIPT_DIR}` is particularly useful in connection scripts to resolve database files relative to the script location,
eliminating the dependency on the working directory.

Before:

```yaml
# Only works when running `spec` from the right directory
SQLite defaults:
  file: db/goals.db
```

After:

```yaml
# Works from any directory
SQLite defaults:
  file: ${SCRIPT_DIR}/goals.db
```

## Relation to SCRIPT_TEMP_DIR

`${SCRIPT_DIR}` and `${SCRIPT_TEMP_DIR}` serve different purposes:

| Variable | Points to | Lifecycle | Use case |
|----------|-----------|-----------|----------|
| `${SCRIPT_DIR}` | Directory containing the `.spec.yaml` or `.spec.md` file | Stable, always the same for a given script | Reference data files, configs, and resources shipped alongside the script |
| `${SCRIPT_TEMP_DIR}` | A temporary directory created on demand | Ephemeral, deleted when the process exits | Store intermediate files, downloads, generated artifacts |

`${SCRIPT_DIR}` is always set. `${SCRIPT_TEMP_DIR}` is only created when first accessed (e.g., by `Temp file`).

```yaml specscript
Code example: SCRIPT_DIR and SCRIPT_TEMP_DIR are different

Temp file:
  filename: marker.txt
  content: x

Assert that:
  not:
    item: ${SCRIPT_DIR}
    equals: ${SCRIPT_TEMP_DIR}
```
