# Package Distribution and Import

This proposal addresses the Tier 2 question from [pluggable-modules.md](pluggable-modules.md): how are shareable
SpecScript packages distributed and imported?

## Current State

Today, SpecScript has a flat, file-based import system:

- **Same-directory auto-discovery**: any `.spec.yaml` in the same directory is callable by name.
- **Imports in `specscript-config.yaml`**: explicit relative file paths like `imports: [../goals/create.spec.yaml]`.
- **`Run script` command**: explicit invocation by filename.

**Problems with the current model:**

1. **No namespacing.** Imported commands are registered by bare filename. Two packages with `list.spec.yaml` collide
   silently.
2. **File-path imports are fragile.** Moving a file breaks every importer. Relative paths like
   `../../packages/foo/bar.spec.yaml` are ugly and brittle.
3. **No directory import.** Each file must be listed individually, so a package with 10 commands means 10 import lines.
4. **No package identity.** There's no way to declare what a package is, what it exports, or what version it is.
5. **No collision detection.** If two imports produce the same canonical command name, the last one wins silently.

## Design Space Analysis

Three established models from existing ecosystems, analyzed for SpecScript fit.

### Option A: Unix PATH Model

**How it works:** A search path (`SPECSCRIPT_PATH` env var or config list) contains directories. SpecScript scans
them for `.spec.yaml` files, like `$PATH` for executables.

```yaml
# specscript-config.yaml
package-path:
  - ~/.specscript/packages
  - /opt/specscript/packages
  - ./local-packages
```

All `.spec.yaml` files found on the path become available as commands globally.

**Installation:** Clone/download a package directory into any path location.

```bash
git clone https://github.com/acme/specscript-api-tools.git ~/.specscript/packages/acme-api
```

**Naming:** Flat, no namespacing. First match on the path wins (like `$PATH`).

| Pros | Cons |
|------|------|
| Familiar to Unix users | No namespacing — collision-prone at scale |
| Simple mental model | Global state is hard to reason about |
| Zero config for discovery | No way to control what a package exports |
| Easy to install | Version conflicts: two packages on path can't coexist at different versions |
| Matches SpecScript's CLI spirit | Implicit ordering determines precedence — subtle bugs |

**Verdict:** Works for personal toolboxes but breaks down with shared/team usage. The collision problem is real: `spec
service-a list-items` and `spec service-b list-items` would both try to register `List items`. Unix works because
executables tend to have unique names (`grep`, `awk`, `sed`). SpecScript commands won't — they're high-level verbs like
`List`, `Create`, `Deploy`.

### Option B: Java-Style Package Imports

**How it works:** Packages have a hierarchical qualified name. Commands are referenced by fully-qualified path or
imported into the local namespace.

```yaml
# specscript-config.yaml
imports:
  - package: digitalai
    use:
      - platform.cloud-connector.Create agent
      - release.folders.List
```

Usage:

```yaml
# With explicit import
- Create agent:
    alias: my-agent

# Fully qualified (always works, no import needed)
- digitalai.release.folders.List:
```

**Package identity:** The directory tree IS the namespace hierarchy. The root `specscript-config.yaml` declares the
package name. Subdirectories form the qualified path.

| Pros | Cons |
|------|------|
| Explicit — you know exactly what you're importing | Heavier ceremony for simple cases |
| Collision-free with qualified names | Requires a registry or convention for name-to-path mapping |
| Scales to large teams and many packages | Verbose for quick scripts |
| Familiar to Java/Go developers | |
| Hierarchy maps naturally to CLI navigation | |

**Verdict:** Good ideas — qualified names, selective imports. The key insight is that the directory hierarchy
already provides natural namespace structure in SpecScript. The `digitalai` sample proves this: its
`platform/cloud-connector/` path is already a de facto qualified path.

### Option C: TypeScript/ES Module Style (File/Directory Imports with Aliasing)

**How it works:** Import by path (relative or from a packages directory), with optional aliasing to handle collisions.

```yaml
# specscript-config.yaml
imports:
  - from: acme-api-tools
  - from: ./local-scripts
    as: local
```

| Pros | Cons |
|------|------|
| Explicit imports — no hidden globals | No natural hierarchy — flat aliasing |
| Directory import with optional alias | Aliases are arbitrary, not structural |
| Selective imports for precision | Doesn't leverage directory structure |
| Path-based — no abstract name registry needed | |

**Verdict:** Good middle ground for flat packages. But it doesn't address the hierarchical case (digitalai)
well. Aliasing is a band-aid for the lack of structural naming.

## The Hierarchy Problem

The v1 proposal said "a directory is a package, subdirectories are internal." This is wrong. Consider the
`samples/digitalai` directory — a real package with real usage:

```
digitalai/
├── specscript-config.yaml          # package root
├── platform/
│   ├── specscript-config.yaml
│   ├── accounts/
│   │   ├── create-account.spec.yaml
│   │   ├── list-accounts.spec.yaml
│   │   └── specscript-config.yaml
│   ├── cloud-connector/
│   │   ├── create-agent.spec.yaml
│   │   ├── list-agents.spec.yaml
│   │   └── specscript-config.yaml
│   ├── credentials/
│   │   ├── login.spec.yaml
│   │   └── specscript-config.yaml
│   └── llm/
│       └── get-models.spec.yaml
└── release/
    ├── specscript-config.yaml
    ├── folders/
    │   ├── list.spec.yaml
    │   ├── move.spec.yaml
    │   └── specscript-config.yaml
    ├── export/
    │   ├── export.spec.yaml
    │   └── import.spec.yaml
    └── cloud-connector/
        ├── configure-remote-runner.spec.yaml
        └── specscript-config.yaml
```

This package has:
- **Two top-level products:** `platform` and `release`
- **Deep nesting:** `platform/cloud-connector/create-agent.spec.yaml` is 3 levels down
- **Name collisions:** Both `platform/cloud-connector/` and `release/cloud-connector/` exist
- **Scripts at every level:** Some directories have scripts, some are just organizational

CLI usage already navigates this naturally: `spec digitalai release folders list`. But there's no way to import
`digitalai.release.folders.List` into a script. The flat import model forces you to import individual files by
relative path.

**The directory hierarchy IS the namespace.** SpecScript already treats directories as CLI subcommand groups. The
package system should extend this to script-level imports, not fight it.

## Recommendation: Hierarchical Packages with Dot-Notation

The directory tree defines the package's namespace structure. Dot notation provides fully-qualified command names.
Explicit imports bring commands into the local namespace for convenience.

### Core Concepts

**A package is a directory tree.** The root has a `specscript-config.yaml` with `Package info`. Every subdirectory
with `.spec.yaml` files provides commands namespaced under that directory path.

**Every command has a fully-qualified name.** The FQN is the package name, followed by the directory path from the
package root, followed by the command name, all joined with dots:

```
digitalai.platform.cloud-connector.Create agent
digitalai.release.folders.List
digitalai.platform.llm.Get models
```

**FQNs always work without imports.** If a package is installed (on the package search path), you can use any of its
commands by FQN in any script, no import needed. This is like Java's `new java.util.ArrayList()` — verbose but
always available.

**Imports bring commands into the local namespace.** To avoid writing FQNs everywhere, you import specific commands
or entire directories:

```yaml
imports:
  - package: digitalai
    use:
      - release.folders.List              # import one command
      - platform.cloud-connector          # import all commands from a directory
```

### Fully-Qualified Command Names

The dot is the separator. In a YAML command key:

```yaml
# Fully qualified — no import needed, always works
- digitalai.release.folders.List:

# Unqualified — works if 'List' was explicitly imported from release.folders
- List:
```

**Why dots and not slashes or spaces?**

- **Dots** are the universal namespace separator (Java, Python, C#, Go, TypeScript). Anyone reading
  `digitalai.platform.cloud-connector.Create agent` immediately understands the structure.
- **Slashes** would conflict with YAML values and look like file paths, which we're trying to get away from.
- **Spaces** are already used within command names (`Create agent`, `List accounts`). Using spaces for both
  namespacing and command words creates ambiguity: is `platform cloud connector Create agent` a command named
  `platform cloud connector Create agent` or the command `Create agent` in directory `platform.cloud-connector`?

The dot creates a clean boundary between the directory path and the command name:

```yaml
- digitalai.platform.cloud-connector.Create agent:
    alias: my-agent
#   └── directory path ────────────┘ └─ command ─┘
```

### Import Syntax

The old relative file-path import syntax (`- ../goals/create.spec.yaml`) is removed. All imports use the new
package-based syntax.

```yaml
# specscript-config.yaml

imports:
  # Import specific commands by qualified path
  - package: digitalai
    use:
      - platform.cloud-connector.Create agent
      - platform.cloud-connector.List agents
      - release.folders.List

  # Import an entire directory (all commands in that directory, non-recursive)
  - package: digitalai
    use:
      - platform.cloud-connector    # imports Create agent, List agents, etc.
      - release.folders             # imports List, Move, etc.

  # Import with alias to resolve collisions
  - package: digitalai
    use:
      - platform.cloud-connector as platform-cc   # Create agent → platform-cc.Create agent
      - release.cloud-connector as release-cc     # Configure remote runner → release-cc.Configure remote runner

  # Import everything from a package (use with care)
  - package: digitalai
    use: all
```

### How Imports Work

**Specific command import** (`platform.cloud-connector.Create agent`): Registers `Create agent` as a local
command. If this collides with another imported command or a local file, it's an error.

**Directory import** (`platform.cloud-connector`): Registers all exported `.spec.yaml` files in that directory as
local commands. Non-recursive — only direct children, not subdirectories. To import commands from subdirectories,
list them explicitly. This prevents accidentally importing 50 commands when you meant 5.

**Aliased import** (`platform.cloud-connector as platform-cc`): Registers each command with the alias as a
dot-prefix. `Create agent` becomes `platform-cc.Create agent`. The alias replaces the directory path, not the
package name.

**Import all** (`use: all`): Imports all exported commands from all directories in the package. Only recommended for
small, focused packages. For something like `digitalai` with 30+ commands across multiple directories, this would
pollute the namespace.

### Usage Examples

Given the `digitalai` package is installed:

**Fully qualified (no imports needed):**

```yaml
- digitalai.release.folders.List:
- As: folders
- Print: "Found ${folders.size} folders"

- digitalai.platform.cloud-connector.Create agent:
    alias: my-runner
```

**With selective imports:**

```yaml
# specscript-config.yaml
imports:
  - package: digitalai
    use:
      - platform.cloud-connector.Create agent
      - release.folders.List
```

```yaml
# In a script — imported commands used by short name
- List:
- As: folders

- Create agent:
    alias: my-runner
```

**With aliased imports (resolving the cloud-connector collision):**

```yaml
# specscript-config.yaml
imports:
  - package: digitalai
    use:
      - platform.cloud-connector as pcc
      - release.cloud-connector as rcc
```

```yaml
# In a script
- pcc.Create agent:
    alias: my-runner

- rcc.Configure remote runner:
    host: https://release.example.com
    token: ${env.RUNNER_TOKEN}
```

**Mixed: some imported, some qualified:**

```yaml
# specscript-config.yaml
imports:
  - package: digitalai
    use:
      - release.folders    # import all folder commands
```

```yaml
# In a script — List is imported, Create agent is qualified
- List:
- As: folders

- digitalai.platform.cloud-connector.Create agent:
    alias: my-runner
```

### Package Definition

A package is a directory tree with a `specscript-config.yaml` at the root that declares `Package info`. This
distinguishes a package root from a regular directory config (which uses `Script info`):

```yaml
# digitalai/specscript-config.yaml
Package info:
  name: digitalai
  version: 2.1.0
Script info: Digital.ai product integrations
```

**Rules:**

- `Package info` with `name` is required for distributable packages. The name must be lowercase letters, digits, and
  hyphens. No dots (dots are the namespace separator).
- Subdirectories with `.spec.yaml` files provide commands. Their path relative to the package root forms the
  qualified path (directory names joined with dots, with spaces and underscores replaced by hyphens).
- `tests/` directories are excluded from command discovery. This is a reserved directory name for test
  infrastructure. If you need a functional directory named "test" (e.g., for a test runner tool), choose an
  alternative like `test-runner/`.
- Directories can also be excluded from command discovery using the existing `hidden: true` property in their
  `specscript-config.yaml`.
- Each subdirectory may have its own `specscript-config.yaml` for metadata (description, etc.) — this is already
  the case today.

**Export control:** By default, all `.spec.yaml` files in a directory are exported. To restrict, add
`exports` in the directory's `specscript-config.yaml`:

```yaml
# platform/cloud-connector/specscript-config.yaml
Script info: Manage Cloud Connector installations.

exports:
  - Create agent
  - List agents
  # create-cloud-connector.spec.yaml exists but is NOT exported (internal helper)
```

If `exports` is absent, all `.spec.yaml` files in that directory are exported.

### Package Search Path

Packages are resolved by name using a search path.

**Default search path** (in order):

1. `./packages/` — project-local packages
2. `~/.specscript/packages/` — user-global packages

**Custom search path** via environment variable:

```bash
export SPECSCRIPT_PACKAGE_PATH="./packages:~/shared-packages:/opt/specscript/packages"
```

**Resolution:** `package: digitalai` → look for directory named `digitalai` in each search path entry. First match
wins. The directory name must match the `name` in `Package info`; warn if they differ.

**No nested packages.** A search path entry contains package directories as immediate children. If a directory within
a search path entry contains multiple packages, they must be siblings, not nested. A `specscript-config.yaml` with
`Package info` found inside an already-resolved package directory is an error — it indicates an illegal nested
package.

```
packages/
├── digitalai/           # OK — package
│   ├── specscript-config.yaml (Package info: {name: digitalai})
│   └── platform/        # OK — directory within digitalai
├── k8s-tools/           # OK — sibling package
│   └── specscript-config.yaml (Package info: {name: k8s-tools})
└── bad-idea/            # OK as package root
    ├── specscript-config.yaml (Package info: {name: bad-idea})
    └── nested-pkg/
        └── specscript-config.yaml (Package info: {name: nested-pkg})  # ERROR — nested package
```

A git repo can contain multiple packages only as siblings at its root level.

### Packaging and Distribution

**Format: Plain directories in git repos.**

A SpecScript package is a directory tree. It's distributed as a git repository (or a subdirectory of one). No
archives, no zips, no compiled artifacts.

**Installation:**

```bash
# Clone a package into your project's packages directory
git clone https://github.com/acme/digitalai-specscript.git packages/digitalai

# Or into your global packages directory
git clone https://github.com/acme/digitalai-specscript.git ~/.specscript/packages/digitalai

# Or use git submodules for version pinning
git submodule add https://github.com/acme/digitalai-specscript.git packages/digitalai
```

**Why not zip/tar?**

- Git repos are the de facto standard for distributing code without a package manager.
- Git submodules provide version pinning for free.
- Developers already know `git clone`.

**Why not a manifest/lockfile?**

- We're explicitly not building a package manager.
- Git submodules already record exact commit hashes.
- For teams that need reproducibility, `git submodule` is the answer.

**Future path:** If SpecScript grows to the point where a package manager is needed, this design is compatible
with one. A future `spec install digitalai` command could automate `git clone` + path management. The package
format (directory tree + `specscript-config.yaml`) wouldn't change.

### CLI and Package Interaction

The CLI already navigates directories hierarchically: `spec digitalai release folders list`. This naturally aligns
with the package's directory structure. The dot-notation in scripts is the programmatic equivalent of the CLI's
space-separated directory traversal:

| CLI invocation | Script command (FQN) |
|---|---|
| `spec digitalai release folders list` | `digitalai.release.folders.List:` |
| `spec digitalai platform cloud-connector create-agent` | `digitalai.platform.cloud-connector.Create agent:` |
| `spec digitalai platform llm get-models` | `digitalai.platform.llm.Get models:` |

The mapping is mechanical: CLI uses kebab-case with spaces between path segments, scripts use Sentence case with
dots between path segments.

### Collision Detection

**At import time**, the runtime checks for command name collisions among:
- Local file commands (same directory)
- Package imports (new `package:` entries)

If two sources register the same canonical command name, it's an error:

```
Error: Command name collision: 'Create agent' is imported from both
  'digitalai.platform.cloud-connector' and 'digitalai.release.cloud-connector'.
Use 'as' alias on one or both imports to disambiguate.
```

**FQN usage never collides** because the full path is unique. Collisions only occur when importing commands into
the local (unqualified) namespace.

### Resolution Order for Command Lookup

Updated resolution order in `FileContext.getCommandHandler()`:

1. Variable assignment syntax
2. Built-in commands (from `CommandLibrary`)
3. Local file commands (same directory — unchanged)
4. Imported package commands (new — from `package:` imports)
5. Fully-qualified package command (new — `package.dir.Command` syntax, resolved via search path)
6. Error: `Unknown command: $command`

Step 5 is the key addition: any command containing a dot is treated as a potential FQN and resolved via the package
search path. This means FQNs work without any import declaration.

### Breaking Changes

- **Relative file-path imports removed.** The old `imports: [../goals/create.spec.yaml]` syntax is no longer
  supported. All imports use the `package:` syntax. Existing projects must migrate to package-based imports. The
  `Run script` command (`Run script: some-file.spec.yaml`) is not affected — it uses file paths by design and
  serves a different purpose (explicit script execution vs. command registration).

### Transitive Dependencies

Not supported. If package A imports package B, and you import package A, you do NOT get package B's commands. Each
consuming script/package must declare its own imports.

This is deliberate. Transitive dependencies require a dependency resolver, and we're not building one.

### Summary of Changes

| What | Change |
|------|--------|
| `specscript-config.yaml` | New section: `Package info` (with `name`, `version`). New field: `exports` |
| `imports` syntax | New `package:` with `use:` (specific, directory, aliased, all). Old file-path imports removed. |
| Command names | Dot-notation FQNs: `package.dir.Command` |
| Package search path | `./packages/`, `~/.specscript/packages/`, configurable via `SPECSCRIPT_PACKAGE_PATH` |
| Command resolution | FQN resolution added as step 5; package imports as step 4 |
| Collision detection | Error on duplicate command names from different import sources |
| Distribution format | Convention: directory trees in git repos |
| Directory exclusion | `tests/` dirs excluded; `hidden: true` also excludes from command discovery |
| Nested packages | Explicitly an error — packages must be siblings in a search path entry |
