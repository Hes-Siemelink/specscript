# Command: Run script

Use **Run script** to run another SpecScript script. See
also [SpecScript files as commands](SpecScript%20files%20as%20commands.spec.md)

| Input  | Supported    |
|--------|--------------|
| Value  | no           |
| List   | auto-iterate |
| Object | yes          |

[Run script.schema.yaml](schema/Run%20script.schema.yaml)

## Basic usage

Suppose you have a cli file `do-something.spec.yaml`

```yaml temp-file=do-something.spec.yaml
Output: Something done!
```

Then you can call it from another SpecScript file using **Run script**.

Pass the name of the script in the current working directory as a single text parameter.

```yaml specscript
Code example: Call another SpecScript file

Run script: do-something.spec.yaml

Expected output: Something done!
```

## Passing input parameters

Suppose you have a cli file `create-greeting.spec.yaml`

```yaml temp-file=create-greeting.spec.yaml
Output: Hello ${input.name}!
```

To call it with **Run script**, you can pass the input parameters in the `input` property. The script name is passed in
the `resource` property.

```yaml specscript
Code example: Call another SpecScript file

Run script:
  resource: create-greeting.spec.yaml
  input:
    name: Alice

Expected output: Hello Alice!
```

You can also pass a list and the script will be called once for each item in the list.

```yaml specscript
Code example: Call another SpecScript file with a list

Run script:
  resource: create-greeting.spec.yaml
  input:
    - name: Alice
    - name: Bob

Expected output:
  - Hello Alice!
  - Hello Bob!
```

## Finding the script

In the example above, we used the property `resource` to indicate that the script to be called was in the same directory
as the current script. It also works with subdirectories. Given a script `subdir/say-hello.spec.yaml`:

```yaml temp-file=subdir/say-hello.spec.yaml
Output: Hello ${input.name}!
```

```yaml specscript
Code example: Call a script in a subdirectory

Run script:
  resource: subdir/say-hello.spec.yaml
  input:
    name: Clarice

Expected output: Hello Clarice!
```

Use the `file` property to look for a script relative to the working directory (the directory you are calling SpecScript
from) instead of the script's own directory.

