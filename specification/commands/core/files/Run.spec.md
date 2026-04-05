# Command: Run

Use **Run** to execute a SpecScript script file or inline script block with variable isolation.

| Input  | Supported    |
|--------|--------------|
| Value  | yes          |
| List   | auto-iterate |
| Object | yes          |

[Run.schema.yaml](schema/Run.schema.yaml)

## Basic usage

Given a script file `do-something.spec.yaml`:

```yaml temp-file=do-something.spec.yaml
Output: Something done!
```

Pass the filename as a value to run it:

```yaml specscript
Code example: Run a script file

Run: do-something.spec.yaml

Expected output: Something done!
```

## Passing input

Use the object form with `script` and `input` to pass input parameters.

Given a script file `create-greeting.spec.yaml`:

```yaml temp-file=create-greeting.spec.yaml
Output: Hello ${input.name}!
```

Provide the input parameters by way of the `input` property:

```yaml specscript
Code example: Run a script with input

Run:
  script: create-greeting.spec.yaml
  input:
    name: Alice

Expected output: Hello Alice!
```

A list input runs the script once per item:

```yaml specscript
Code example: Run a script with list input

Run:
  script: create-greeting.spec.yaml
  input:
    - name: Alice
    - name: Bob

Expected output:
  - Hello Alice!
  - Hello Bob!
```

## Working directory

Use `cd` to run a script from a different directory. The script file is resolved relative to `cd` instead of the current
script's directory.

```yaml specscript
Code example: Run with cd

Temp file:
  name: hello.spec.yaml
  content:
    Output: Hello from temp!

Run:
  cd: ${SCRIPT_TEMP_DIR}
  script: hello.spec.yaml

Expected output: Hello from temp!
```

## Running a temp file

`Temp file` returns the absolute path of the created file. Use `file` to run it directly:

```yaml specscript
Code example: Run a temp file

Temp file:
  name: greetings.spec.yaml
  content:
    Output: Greetings!

Run:
  file: ${output}

Expected output: Greetings!
```

## Inline script

Use `script` with an object value to run inline commands with variable isolation:

```yaml specscript
Code example: Run inline script

${name}: World

Run:
  script:
    Output: Hello!

Expected output: Hello!
```

The inline script runs in a clean variable scope — parent variables are not visible inside the block.

Combine with `cd` to run inline commands that operate on files in a different directory:

```yaml specscript
Code example: Run inline script with cd

Temp file:
  name: data.txt
  content: some data

Run:
  cd: ${SCRIPT_TEMP_DIR}
  script:
    Read file: data.txt

Expected output: some data
```

## Output

The output of `Run` is the output of the last command executed — same as running a script.

```yaml specscript
Code example: Output of Run

Run:
  script:
    - Output: first
    - Output: second

Expected output: second
```

## File resolution

The `script` and `file` properties differ in how they resolve paths:

| Property                     | Resolves relative to                |
|------------------------------|-------------------------------------|
| Value form (`Run: foo.yaml`) | Script's directory                  |
| `script: foo.yaml`           | Script's directory (or `cd` if set) |
| `file: /path/to/foo.yaml`    | Working directory (or `cd` if set)  |
