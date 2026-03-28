# Command: Do

Use `Do` to execute one or more other commands.

| Input      | Supported     |
|------------|---------------|
| Scalar     | no            |
| List       | yes           |
| Object     | yes           |
| Raw input  | yes           |

[Do.schema.yaml](schema/Do.schema.yaml)

## Basic usage

Remember that the **SpecScript** syntax is in Yaml, so it is not permitted to repeat a key. This poses a problem in a
programming language, wher you are prone to have several invvocations of the same command, but with different
parameters.

There are multiple ways to execute various commands with the same name in SpecScript.

First there, is **Do**, that takes a list of commands to execute:

```yaml specscript
Code example: Use 'Do' to repeat a command

Do:
  - Print: Hello
  - Print: World!
```

This will give the following output:

```output
Hello
World!
```

The final option is just to stick `---` somewhere in the code to separate the yaml maps. SpecScript will stitch them
together. This only works on the top-level of course.

```yaml specscript
Code example: Use separators

Print: Hello
---
Print: World!
```

## Output

When `Do` executes a list of commands, the output is the result of the **last** command — the same as running a script.

```yaml specscript
Code example: Output of Do is the last command's output

Do:
  - Output: one
  - Output: two
  - Output: three

Expected output: three
```