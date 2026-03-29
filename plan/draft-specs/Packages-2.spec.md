# Packages v2 — Revised Design

This revision addresses three problems discovered during code review of the initial package implementation.

## Problem 1: Full name ambiguity

In `greeter.greetings.Hello`, the first segment `greeter` is a package name. But SpecScript also has local
subdirectories that appear as command groups. If a script lives in a directory with a `greetings/` subdirectory,
there's no syntactic way to tell whether `greetings.Hello` refers to a package named `greetings` or a local
subdirectory.

**Current behavior:** The resolution order silently prefers local files over packages. This means adding a local
directory can shadow a package import with no warning.

**Proposed change:** Full names (the dot-notation form) are only used inside `imports` in `specscript-config.yaml`,
never inline in scripts. In scripts, you use either:

- The short command name (from an import or local file)
- An aliased command name (from an aliased import)

This eliminates the ambiguity entirely. There is no need to parse `greeter.greetings.Hello` inside a YAML command
key — that syntax only appears in the structured `imports` block where the package name is already a separate YAML
key.

If a future need arises for inline full names in scripts, we can introduce an unambiguous syntax like
`[greeter].greetings.Hello` — but we don't need it now.

## Problem 2: The `as` construct in import lists

The current import syntax encodes aliasing inside a string value:

```yaml
imports:
  greeter:
    - greetings as formal
```

This is un-YAML-like — it forces string parsing of what should be structured data. SpecScript already avoids this
pattern elsewhere. The `For each` command originally had a similar `as` construct but it operates on command keys,
not inside list values.

**Proposed change:** Use YAML map syntax for aliased imports:

```yaml
imports:
  greeter:
    - greetings:
        as: formal
    - greetings.Hello           # non-aliased specific import, still a plain string
```

Or, since most imports don't need aliases, keep the common case simple:

```yaml
imports:
  greeter:
    - greetings.Hello           # specific command — plain string
    - greetings                 # directory import — plain string
    - greetings:                # directory import with alias — map entry
        as: formal
```

This keeps the 80% case (no alias) as a simple string, and uses proper YAML structure for the 20% case.

## Problem 3: What `imports` means — packages vs local directories

The current design forces everything through the package concept. The `imports` key in `specscript-config.yaml` is
a map where each key is a package name. But there's a real use case for importing commands from local subdirectories
within the same project — without those directories being declared as packages.

Example: a `tests/` directory needs to call commands from a sibling `helper/` directory. Today this requires either:
- Making `helper/` a package (over-engineering for a test utility)
- Using the old relative file-path imports (removed in v1)

**Proposed change:** Separate the two concerns:

### Package imports

For external packages discovered via search path:

```yaml
imports:
  greeter:                          # package name — resolved via search path
    - greetings.Hello               # specific command from the package
    - greetings                     # all commands from greetings/ directory
    - greetings:                    # aliased directory import
        as: formal
```

The package name is the YAML map key. It must match a `Package info` name found on the search path.

### Local imports

For subdirectories within the same project, without requiring package declarations:

```yaml
imports:
  ./helper:                         # relative path — starts with ./
    - helper-function               # specific command by filename (without extension)
    - call-helper
```

The `./` prefix distinguishes local directory imports from package imports. The path is relative to the directory
containing the `specscript-config.yaml`. Only downward paths are allowed — no `../` references.

This replaces the old file-path imports with a structured, forward-only alternative. It solves the `tests/` →
`helper/` use case without requiring `helper/` to be a package.

## Package search path (revised)

With local imports covering the "project-local" case, the package search path only needs to handle installed
packages:

1. `~/.specscript/packages/` — user-global packages
2. `SPECSCRIPT_PACKAGE_PATH` entries — custom locations (colon-separated)
3. Ancestor walk — scripts inside a package can reference their own package by name

The `./packages/` convention is removed. It conflated "I want to use a package" with "I happen to have a directory
named packages." Package installation is an explicit act (clone into home dir or set env var), not a directory
naming convention.

## Command resolution order (revised)

| Priority | Source                                  |
|----------|-----------------------------------------|
| 1        | Variable assignment syntax              |
| 2        | Built-in commands                       |
| 3        | Local file commands (same directory)    |
| 4        | Imported commands (local + package)     |
| 5        | Error: unknown command                  |

Notably absent: inline FQN resolution. Commands containing dots are no longer treated specially. If you want to use
a package command, import it. This is simpler and avoids the ambiguity problem entirely.

## Import resolution rules

Regardless of source (package or local directory):

- **Specific command** (`greetings.Hello` or `helper-function`): Registers that single command by its short name.
- **Directory import** (`greetings` or `./helper`): Registers all `.spec.yaml` files in that directory.
  Non-recursive — only direct children.
- **Aliased import**: Registers commands with the alias as a prefix: `formal.Hello`, `formal.Goodbye`.
- **Import all** (`greeter: all`): Registers every command from every directory in the package. Only for packages,
  not local directories.
- **Collision detection**: If two imports (from any source) produce the same command name, it's an error at load
  time with a message naming both sources.

## Excluded directories

Unchanged from v1:

- `tests/` directories are excluded from package command discovery.
- Directories with `hidden: true` in their `specscript-config.yaml` are excluded.

Both apply to package scanning only. Local imports explicitly name what they want, so exclusion rules don't apply.

## Nested packages

Unchanged: a package cannot contain another package. Nested `Package info` declarations are an error.

## Sample structure

Given the sample files in `specification/language/package-samples/`:

```
package-samples/
├── specscript-config.yaml          # imports for test scripts
├── test-packages.spec.yaml         # test script
└── packages/                       # ← only needed if using ./packages/ convention
    ├── greeter/
    │   ├── specscript-config.yaml  # Package info: greeter
    │   ├── welcome.spec.yaml       # root-level command
    │   ├── greetings/
    │   │   ├── hello.spec.yaml
    │   │   └── goodbye.spec.yaml
    │   ├── tests/                  # excluded from discovery
    │   └── internal/               # hidden: true
    └── alt-greeter/
        ├── specscript-config.yaml  # Package info: alt-greeter
        └── greetings/
            └── hello.spec.yaml
```

With the revised design, the `specscript-config.yaml` would look like:

```yaml
imports:
  greeter:
    - greetings:
        as: formal
  alt-greeter:
    - greetings:
        as: casual
```

## Migration from v1

| v1 syntax | v2 syntax |
|-----------|-----------|
| `greeter.greetings.Hello:` (inline FQN in script) | Import first, then use `Hello:` or `formal.Hello:` |
| `- greetings as formal` (string with `as`) | `- greetings:` with `as: formal` (YAML map) |
| `./packages/` discovery | `SPECSCRIPT_PACKAGE_PATH` or `~/.specscript/packages/` |
| `- ../goals/create.spec.yaml` (old file paths) | `./goals:` with `- create` (local import) |

## Open questions

1. **Should local imports support directory imports?** The `./helper` syntax imports a directory. Should
   `./helper/sub` also work for nested subdirectories? Proposal: yes, using forward slash as the path separator
   since these are filesystem paths.

2. **Should `imports` support both local and package in the same block?** The `./` prefix distinguishes them, so
   they can coexist in one `imports` map. But should they be separate keys (`imports` vs `local-imports`) for
   clarity? Proposal: single `imports` block — the `./` prefix is clear enough.

3. **Alias syntax alternatives.** Instead of `as:` inside a map, we could use YAML anchors, a different key name,
   or a convention like `greeter/greetings -> formal`. The map syntax (`greetings:` with `as: formal`) is the most
   YAML-native option.
