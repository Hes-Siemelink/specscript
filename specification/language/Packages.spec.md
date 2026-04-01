# Packages

A package is a distributable collection of SpecScript commands. Packages let you share and import scripts across
projects.

## Package declaration

A directory becomes a package by declaring `Package info` in its `specscript-config.yaml`. The name of the package is
the name of the directory it's in.

The examples in this document use a package called `greetings` stored in `lib/greetings/`. The `lib` directory serves as
a package library — a directory that contains one or more packages.

The `greetings` package's `specscript-config.yaml` looks like this:

```yaml temp-file=lib/greetings/specscript-config.yaml
Package info: Different ways to greet people
```

The package contains a command `lib/greetings/hello.spec.yaml` that prints a simple greeting:

```yaml temp-file=lib/greetings/hello.spec.yaml
Input schema:
  type: object
  properties:
    name:
      type: string
      default: World

Output: Hello ${name}!
```

We can run it directly to see what it does:

```cli cd=${SCRIPT_TEMP_DIR}
spec lib/greetings/hello.spec.yaml
```

Output:

```output
Hello World!
```

## Importing a command from a package

Now we want to use the `hello` command from another script that lives outside the package.

To use commands from a package, we need to import them. This is done by adding an `imports` section in
`specscript-config.yaml`.

For example:

```yaml temp-file=simple-import/specscript-config.yaml
imports:
  greetings:
    - hello
```

This will import the `hello` command from the `greetings` package. The key `greetings` matches the package name, and the
list of items specifies which commands to import from that package.

We can now use the Hello command directly in a script. Here's `run.spec.yaml`:

```yaml temp-file=simple-import/run.spec.yaml
Hello:
  name: Alice
```

One last thing to do before running it is to tell SpecScript where the package can be found. SpecScript has various ways
to find packages. The simplest is to pass it via the `--package-path` flag (shorthand: `-p`). In this case we point it
to the `lib` directory where the `greetings` package is installed:

```cli cd=${SCRIPT_TEMP_DIR}
spec --package-path lib simple-import/run
```

Output:

```output
Hello Alice!
```

## Subdirectories in packages

The greetings package also has a subdirectory with two scripts.

`lib/greetings/sub/hi.spec.yaml`:

```yaml temp-file=lib/greetings/sub/hi.spec.yaml
Input schema:
  type: object
  properties:
    name:
      type: string

Output: Hi ${name}!
```

`lib/greetings/sub/bye.spec.yaml`:

```yaml temp-file=lib/greetings/sub/bye.spec.yaml
Input schema:
  type: object
  properties:
    name:
      type: string

Output: Bye ${name}!
```

### Importing a specific command from a subdirectory

Point to a single command from a package subdirectory to import it.

The config `subdir-import-single/specscript-config.yaml`:

```yaml temp-file=subdir-import-single/specscript-config.yaml
imports:
  greetings:
    - sub/hi
```

Now we create a script `subdir-import-single/hi-bob.spec.yaml`:

```yaml temp-file=subdir-import-single/hi-bob.spec.yaml
Hi:
  name: Bob
```

And run it. The shorthand `-p` is equivalent to `--package-path`.

```cli cd=${SCRIPT_TEMP_DIR}
spec -p lib subdir-import-single/hi-bob
```

```output
Hi Bob!
```

## Importing all commands from a subdirectory

Importing a directory name registers all `.spec.yaml` files in that directory as commands. Non-recursive — only direct
children are included.

The config `subdir-import-all/specscript-config.yaml`:

```yaml temp-file=subdir-import-all/specscript-config.yaml
imports:
  greetings:
    - sub
```

The script `subdir-import-all/hi-and-bye.spec.yaml`:

```yaml temp-file=subdir-import-all/hi-and-bye.spec.yaml
Hi:
  name: Alice
As: greeting
---
Bye:
  name: Alice
As: farewell
---
Output: "${greeting} ${farewell}"
```

Run it:

```cli cd=${SCRIPT_TEMP_DIR}
spec -p lib subdir-import-all/hi-and-bye
```

```output
Hi Alice! Bye Alice!
```

## Aliased imports

When importing a single command, assign an alias using YAML map syntax with `as:`. The command is registered under the
alias name instead of its original name.

The config `test-alias/specscript-config.yaml`:

```yaml temp-file=test-alias/specscript-config.yaml
imports:
  greetings:
    - sub/hi:
        as: greet
```

The script `test-alias/run.spec.yaml`:

```yaml temp-file=test-alias/run.spec.yaml
Greet:
  name: Carol
```

```cli cd=${SCRIPT_TEMP_DIR}
spec -p lib test-alias/run
```

```output
Hi Carol!
```

## Wildcard imports

Use `*` to import all commands from the root of a package, or scope it to a subdirectory with `sub/*`. Use `**` to
import recursively, including all subdirectories.

A bare directory name like `sub` is equivalent to `sub/*`.

As a shorthand, `greetings: "*"` and `greetings: "**"` can be used instead of a list when no other imports are needed.
A bare reference without items (`greetings:` with no value) is equivalent to `greetings: "*"`.

### Directory import with `*`

The following `specscript-config.yaml` imports root-level commands using the shorthand:

```yaml temp-file=import-root-star/specscript-config.yaml
imports:
  greetings: "*"
```

The script `hello-again.spec.yaml`:

```yaml temp-file=import-root-star/run.spec.yaml
Hello: { }
```

Run it:

```cli cd=${SCRIPT_TEMP_DIR}
spec -p lib import-root-star/run
```

And we see familiar output:

```output
Hello World!
```

### Bare reference

A bare source reference with no value imports all root-level commands. The config `import-bare/specscript-config.yaml`:

```yaml temp-file=import-bare/specscript-config.yaml
imports:
  greetings:
```

The script `import-bare/run.spec.yaml`:

```yaml temp-file=import-bare/run.spec.yaml
Hello: { }
```

```cli cd=${SCRIPT_TEMP_DIR}
spec -p lib import-bare/run
```

```output
Hello World!
```

The config `specscript-config.yaml` imports all commands from `sub/`:

```yaml temp-file=import-subdir-star/specscript-config.yaml
imports:
  greetings:
    - "sub/*"
```

The script `hi-dave.spec.yaml`:

```yaml temp-file=import-subdir-star/hi-dave.spec.yaml
Hi:
  name: Dave
```

Run it:

```cli cd=${SCRIPT_TEMP_DIR}
spec -p lib import-subdir-star/hi-dave
```

With output:

```output
Hi Dave!
```

### Recursive import with `**`

This config `specscript-config.yaml` imports everything recursively using the shorthand:

```yaml temp-file=import-root-globstar/specscript-config.yaml
imports:
  greetings: "**"
```

The script `hi-eve.spec.yaml` can use commands from any level:

```yaml temp-file=import-root-globstar/run.spec.yaml
Hi:
  name: Eve
```

Run it:

```cli cd=${SCRIPT_TEMP_DIR}
spec -p lib import-root-globstar/run
```

With output:

```output
Hi Eve!
```

## Local imports

To import commands from a subdirectory within the same project, use the `./` prefix. No package declaration is needed.
For importing from sibling or parent directories, use package imports with self-package discovery instead.

### Local file import

Given a helper script `import-local/helper/say-something.spec.yaml`:

```yaml temp-file=import-local/helper/say-something.spec.yaml
Input schema:
  type: object
  properties:
    what:
      type: string

Output: Something ${what}
```

The config `import-local/specscript-config.yaml` imports from the local `helper` directory:

```yaml temp-file=import-local/specscript-config.yaml
imports:
  ./helper:
    - say-something
```

The script `something-funny.spec.yaml`:

```yaml temp-file=import-local/something-funny.spec.yaml
Say something:
  what: funny
```

Local imports don't need `-p` — the path is relative to the config directory:

```cli cd=${SCRIPT_TEMP_DIR}/import-local
spec something-funny
```

Output:

```output
Something funny
```

### Local directory import

A local import with `*` imports every `.spec.yaml` file from that directory.

Given `import-local-all/utils/greet.spec.yaml`:

```yaml temp-file=import-local-all/utils/greet.spec.yaml
Output: Hello!
```

And `import-local-all/utils/farewell.spec.yaml`:

```yaml temp-file=import-local-all/utils/farewell.spec.yaml
Output: Goodbye!
```

The config `import-local-all/specscript-config.yaml`:

```yaml temp-file=import-local-all/specscript-config.yaml
imports:
  ./utils:
    - "*"
```

The script `import-local-all/run.spec.yaml`:

```yaml temp-file=import-local-all/run.spec.yaml
Greet: { }
```

```cli cd=${SCRIPT_TEMP_DIR}
spec import-local-all/run
```

```output
Hello!
```

## Import scope

Imported commands retain access to their own local imports, but those imports do not leak to the caller.

The `greetings` package's `sub/` directory has its own config importing a local helper. The helper
`lib/greetings/sub/internal/format-name.spec.yaml`:

```yaml temp-file=lib/greetings/sub/internal/format-name.spec.yaml
Input schema:
  type: object
  properties:
    name:
      type: string

${formatted}: "<<${name}>>"

Output: ${formatted}
```

The config `lib/greetings/sub/specscript-config.yaml` imports the helper locally:

```yaml temp-file=lib/greetings/sub/specscript-config.yaml
imports:
  ./internal:
    - format-name
```

The command `lib/greetings/sub/fancy-hi.spec.yaml` uses the local import:

```yaml temp-file=lib/greetings/sub/fancy-hi.spec.yaml
Input schema:
  type: object
  properties:
    name:
      type: string

Format name:
  name: ${name}
As: formatted

Output: Fancy hi ${formatted}!
```

A project imports only `fancy-hi` from the package. The config `test-scope/specscript-config.yaml`:

```yaml temp-file=test-scope/specscript-config.yaml
imports:
  greetings:
    - sub/fancy-hi
```

The script `test-scope/run.spec.yaml` can use `Fancy hi` — the command resolves its own local import internally:

```yaml temp-file=test-scope/run.spec.yaml
Fancy hi:
  name: Frank
```

```cli cd=${SCRIPT_TEMP_DIR}
spec -p lib test-scope/run
```

```output
Fancy hi <<Frank>>!
```

The caller does NOT have access to `Format name` — it is scoped to the `sub/` directory:

```yaml temp-file=test-scope/leak.spec.yaml
Format name:
  name: Frank
```

```cli cd=${SCRIPT_TEMP_DIR}
spec -p lib test-scope/leak
```

```output error
Scripting error

Unknown command: Format name
```

## Excluded directories

Directories named `tests` are excluded from package command discovery. Directories with `hidden: true` in their
`specscript-config.yaml` are also excluded. These exclusions apply to package scanning only — local imports explicitly
name what they want.

## Nested packages

A package cannot contain another package. A nested `Package info` declaration is an error.

## Command resolution order

| Priority | Source                               |
|----------|--------------------------------------|
| 1        | Variable assignment syntax           |
| 2        | Built-in commands                    |
| 3        | Local file commands (same directory) |
| 4        | Imported commands (local + package)  |
| 5        | Error: unknown command               |

## Package search path

Packages are discovered from the following locations, checked in order:

| Priority | Location                                            |
|----------|-----------------------------------------------------|
| 1        | Parent of enclosing package (auto-discovered)       |
| 2        | `-p` / `--package-path` CLI argument                |
| 3        | `SPECSCRIPT_PACKAGE_PATH` entries (colon-separated) |
| 4        | `~/.specscript/packages/`                           |

### Enclosing package discovery

When a script is inside a package, the parent directory of that package is automatically added to the search path. This
lets scripts within a package import from their own package and sibling packages without `--package-path`.

The `greetings` package has a `tests/` directory with a test script. The test imports from its own package — no
`--package-path` needed because the enclosing package's parent (`lib/`) is auto-discovered.

The test config `lib/greetings/tests/specscript-config.yaml`:

```yaml temp-file=lib/greetings/tests/specscript-config.yaml
imports:
  greetings:
    - hello
```

The test script `lib/greetings/tests/test-hello.spec.yaml`:

```yaml temp-file=lib/greetings/tests/test-hello.spec.yaml
Hello:
  name: Self
```

```cli cd=${SCRIPT_TEMP_DIR}/lib/greetings/tests
spec test-hello
```

```output
Hello Self!
```
