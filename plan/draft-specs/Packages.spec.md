# Packages

A package is a distributable SpecScript library. It is a directory tree whose root `specscript-config.yaml`
declares `Package info`.

## Package definition

A directory becomes a package when its `specscript-config.yaml` contains a `Package info` section with a `name`. This
distinguishes it from a regular directory config that uses `Script info`.

For example, the package root `packages/greeter/specscript-config.yaml`:

```yaml temp-file=packages/greeter/specscript-config.yaml
Package info:
  name: greeter
```

Subdirectories containing `.spec.yaml` files provide commands namespaced under the directory path. The `greetings`
directory has two commands — file `packages/greeter/greetings/hello.spec.yaml`:

```yaml temp-file=packages/greeter/greetings/hello.spec.yaml
Output: Hello ${input.name}!
```

And `packages/greeter/greetings/goodbye.spec.yaml`:

```yaml temp-file=packages/greeter/greetings/goodbye.spec.yaml
Output: Goodbye ${input.name}!
```

## Fully-qualified command names

Every command in a package has a fully-qualified name (FQN): the package name, directory path from the package root, and
command name, joined with dots. FQNs work without imports — if the package is on the search path, any script can use the
FQN directly.

```yaml specscript
Code example: Invoke a package command by FQN

greeter.greetings.Hello:
  name: World

Expected output: Hello World!
```

The FQN maps kebab-case filenames to Sentence-case command names:

| File path                             | FQN                         |
|---------------------------------------|-----------------------------|
| `greeter/greetings/hello.spec.yaml`   | `greeter.greetings.Hello`   |
| `greeter/greetings/goodbye.spec.yaml` | `greeter.greetings.Goodbye` |

## Package search path

Packages are discovered from a search path, checked in order:

1. `./packages/` relative to working directory
2. `~/.specscript/packages/`
3. Entries from `SPECSCRIPT_PACKAGE_PATH` environment variable (colon-separated)

First match wins. The directory name must match the `name` in `Package info`.

## Excluded directories

The `tests/` directory name is reserved and excluded from command discovery. Directories with `hidden: true` are also
excluded.

The `greeter` package has a `tests/` directory and a hidden `internal/` directory. File
`packages/greeter/tests/greeting-tests.spec.yaml`:

```yaml temp-file=packages/greeter/tests/greeting-tests.spec.yaml
Output: test
```

File `packages/greeter/internal/specscript-config.yaml`:

```yaml temp-file=packages/greeter/internal/specscript-config.yaml
hidden: true
```

File `packages/greeter/internal/helper.spec.yaml`:

```yaml temp-file=packages/greeter/internal/helper.spec.yaml
Output: internal
```

Commands in excluded directories are not accessible by FQN:

```yaml specscript
Code example: Accessible directory works

greeter.greetings.Hello:
  name: test

Expected output: Hello test!
```

```yaml specscript
Code example: tests directory is excluded

greeter.tests.Greeting tests: { }

Expected error: "Unknown command"
```

## Importing package commands

Import commands using `imports` in `specscript-config.yaml`. The package name is the map key, and the value is a list
of what to import.

### Importing specific commands

```yaml
# specscript-config.yaml
imports:
  greeter:
    - greetings.Hello
```

```yaml specscript
Code example: Import a specific command

Hello:
  name: Alice

Expected output: Hello Alice!
```

### Importing a directory

Import all commands from a directory. Directory imports are non-recursive — only direct `.spec.yaml` files in that
directory, not subdirectories.

```yaml
# specscript-config.yaml
imports:
  greeter:
    - greetings
```

```yaml specscript
Code example: Import all commands from a directory

Hello:
  name: Bob

Goodbye:
  name: Bob

Expected output:
  - Hello Bob!
  - Goodbye Bob!
```

### Aliased imports

Use `as` to import under a prefix. This resolves collisions when two packages export commands with the same name.

A second package `alt-greeter` also has a `Hello` command. File `packages/alt-greeter/specscript-config.yaml`:

```yaml temp-file=packages/alt-greeter/specscript-config.yaml
Package info:
  name: alt-greeter
```

File `packages/alt-greeter/greetings/hello.spec.yaml`:

```yaml temp-file=packages/alt-greeter/greetings/hello.spec.yaml
Output: Howdy ${input.name}!
```

```yaml
# specscript-config.yaml
imports:
  greeter:
    - greetings as formal
  alt-greeter:
    - greetings as casual
```

```yaml specscript
Code example: Aliased imports resolve collisions

formal.Hello:
  name: World

casual.Hello:
  name: World

Expected output:
  - Hello World!
  - Howdy World!
```

### Import all

Import all commands from all directories in a package with `all`.

```yaml
# specscript-config.yaml
imports:
  greeter: all
```

```yaml specscript
Code example: Import all commands from a package

Hello:
  name: Carol

Expected output: Hello Carol!
```

## Collision detection

If two unaliased imports produce the same command name, it is an error. The error message names both sources.

```yaml
# specscript-config.yaml — this causes a collision
imports:
  greeter:
    - greetings
  alt-greeter:
    - greetings
```

FQNs never collide — use them or aliased imports to disambiguate.

## Nested packages are not allowed

A package cannot contain another package. If a subdirectory within a resolved package also declares `Package info`, it
is an error.

## Command resolution order

| Priority | Source                          |
|----------|---------------------------------|
| 1        | Variable assignment syntax      |
| 2        | Built-in commands               |
| 3        | Local file commands (same dir)  |
| 4        | Imported package commands       |
| 5        | Fully-qualified package command |
| 6        | Error: unknown command          |

A command containing a dot is treated as an FQN candidate at step 5 if not resolved earlier.
