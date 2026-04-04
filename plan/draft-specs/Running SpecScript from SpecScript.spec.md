# Running SpecScript from SpecScript

SpecScript has three commands for executing SpecScript code from within a script. They form a ladder of increasing
isolation.

| | `Do` | `Run` | `Cli` |
|---|---|---|---|
| Context | Same (shared) | New (isolated) | Separate CLI invocation |
| Variables | Shared with parent | Clean scope | None |
| Commands | From host script | From host script | From target directory |
| Use case | Inline block | Script execution | Full isolation |

## `Do` — inline execution

`Do` executes commands in the current context. Variables are shared, commands are shared, nothing is isolated. Use it
for control flow — repeating a command, conditional blocks, grouping.

```yaml specscript
Code example: Do shares variables

$greeting: Hello

Do:
  - Print: ${greeting}

Expected console output: Hello
```

See **[Do](../commands/core/control-flow/Do.spec.md)** for the full reference.

## `Run` — isolated execution

`Run` executes a script file or inline block with variable isolation. The child script gets a clean variable scope —
only `input` is passed explicitly. The host script's commands, imports, and connections remain available.

```yaml specscript
Code example: Run creates a clean scope

$greeting: Hello

Run:
  script:
    $greeting: Goodbye
    Output: ${greeting}

Expected output: Goodbye
```

Use `cd` to run scripts from a different directory, or `file` to run a script by absolute path (e.g. one created
with `Temp file`).

See **[Run](../commands/core/files/Run.spec.md)** for the full reference.

## `Cli` — full isolation

`Cli` invokes SpecScript as a separate CLI process. The target script sees only its own directory's commands and
configuration. Use this when you need complete isolation — testing CLI behavior, running scripts from unrelated
projects, or verifying that a script works standalone.

```yaml specscript
Code example: Cli runs with full isolation

Cli: spec --help

Expected console output: |
  SpecScript -- Create instantly runnable specs using Yaml and Markdown!
```

See **[Cli](../commands/core/shell/Cli.spec.md)** for the full reference.

## When to use which

- **Grouping commands or repeating a command name** → `Do`
- **Calling a helper script or running code in a different directory** → `Run`
- **Testing CLI behavior or running an unrelated script** → `Cli`
