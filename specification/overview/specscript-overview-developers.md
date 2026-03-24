---
status: draft
ai-generated: true
complete-garbage: false
human-edited: false
---

# SpecScript -- Overview for Developers

## What is SpecScript?

SpecScript is a declarative scripting language built on YAML and Markdown. It lets you write executable
specifications -- files that simultaneously serve as documentation, tests, and runnable scripts. The core philosophy is
simple:
**documentation that can't lie, because it executes**.

Scripts are `.spec.yaml` files (pure YAML) or `.spec.md` files (Markdown with embedded YAML code blocks). Both are
first-class citizens. There is no custom parser; SpecScript rides on standard YAML syntax with a `${variable}`
interpolation layer on top.

## Core Language Concepts

**Commands** are YAML keys that, by convention, start with a capital letter. Matching is case-insensitive. A script is a sequence of commands executed top-to-bottom:

```yaml
Print: Hello world
---
GET: http://api.example.com/items
---
Expected output: [ 1, 2, 3 ]
```

The `---` YAML document separator is used to repeat commands (YAML does not allow duplicate keys).

**Variables** use `${...}` syntax with path notation (`${book.chapters[0].title}`). Every command stores its result in
the implicit `${output}` variable, which can be captured with `As: ${myVar}`.

**Input schema** uses a JSON Schema subset to define script parameters. These auto-generate CLI `--help` output,
interactive prompts, and MCP tool schemas:

```yaml
Input schema:
  type: object
  properties:
    name:
      description: Your name
      default: World
```

**Control flow** includes `If`, `For each`, `Repeat`, `When`, `Do`, and error handling (`On error`, `Expected error`).

## Key Capabilities

- **HTTP client**: `GET`, `POST`, `PUT`, `PATCH`, `DELETE` with headers, body, auth, and request defaults.
- **HTTP server**: Spin up an embedded HTTP server backed by SpecScript handlers -- useful for mocking APIs.
- **MCP server**: Define Model Context Protocol servers with tools, resources, and prompts, all backed by SpecScript.
- **Testing**: `Tests` blocks, `Assert that`, `Assert equals`, `Expected output`. Tests run with `spec --test`.
- **User interaction**: `Prompt` (text, enum, multi-select), `Confirm`, `Prompt object`.
- **File I/O, shell commands, credentials management, data manipulation** (find, sort, add, JSON patch, etc.).

## The Executable Documentation Model

This is the central idea. SpecScript Markdown files (`.spec.md`) contain ` ```yaml specscript` code blocks that
**execute as tests** during `./gradlew specificationTest`. SpecScript's own specification (440+ tests) is written this
way -- every example in the docs is a test. If someone changes behavior and the docs become wrong, the build breaks.

Hidden setup/cleanup code goes in `<!-- yaml specscript -->` HTML comments. Predefined answers for interactive prompts
go in `<!-- answers -->` blocks. Output validation uses ` ```output` blocks.

## Architecture at a Glance

- **Kotlin 2.1 / JVM 21** implementation. Ktor for HTTP, Jackson for JSON/YAML, MCP Kotlin SDK for MCP.
- Commands are singleton objects extending `CommandHandler`, registered in `CommandLibrary`.
- Scripts run in a `ScriptContext` that carries variables, session state, and I/O.
- Two JARs: thin (531KB, needs dependencies) and fat (36MB, self-contained).

## Development Workflow

1. Write the spec first (`.spec.md` with executable examples).
2. Run `./gradlew specificationTest` -- tests fail because the feature doesn't exist yet.
3. Implement in Kotlin.
4. Tests go green. Docs are already written.

This is **spec-driven development**: the specification is the single source of truth for behavior, documentation, and
tests.

## When to Use SpecScript

- **API prototyping and testing**: Script HTTP interactions declaratively, validate responses.
- **Living documentation**: Write specs that stay correct because they run in CI.
- **MCP tool servers**: Define AI tool servers in YAML without writing application code.
- **Interactive CLI tools**: Build prompts and workflows without a UI framework.
- **Integration testing**: Chain HTTP calls, validate data, manage credentials -- all in readable YAML.
