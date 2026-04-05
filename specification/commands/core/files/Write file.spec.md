# Command: Write file

`Write file` saves output to a file.

| Input  | Supported |
|--------|-----------|
| Value  | yes       |
| List   | no        |
| Object | yes       |

[Write file.schema.yaml](schema/Write%20file.schema.yaml)

## Writing content to a file

Use **Write file** to store content in a file

```yaml specscript
Code example: Write content to a file

Write file:
  file: ${SCRIPT_TEMP_DIR}/greeting.txt
  content: Hello, World!

Read file: ${SCRIPT_TEMP_DIR}/greeting.txt

Expected output: Hello, World!
```

## Short version

You can store the contents of the `${output}` variable directly with the short form.

```yaml specscript
Code example: Save output

Output: Hello, World!

Write file: ${SCRIPT_TEMP_DIR}/greeting.txt
```

