---
name: specscript-specs
description: How to write SpecScript specification documents (.spec.md). Code block types, rules, sample server endpoints, file extensions. Use when creating or editing specification files in the specification/ directory.
compatibility: Requires the spec CLI and the specscript project structure.
metadata:
  author: specscript
  version: "1.0"
---

## Overview

SpecScript documentation IS the test suite. Every code example in specification documents executes during
`./gradlew specificationTest`. You cannot lie in documentation — if you write it, it must work or tests fail.

The files in `specification/language/` are the authoritative reference for SpecScript syntax:

- `SpecScript Yaml Scripts.spec.md` — core language syntax and command usage
- `SpecScript Markdown Documents.spec.md` — proper structure for spec files including hidden setup/cleanup
- `Organizing SpecScript files in directories.spec.md` — file organization patterns

## Code block types

- `` ```yaml specscript `` — **EXECUTABLE**: runs as tests during `specificationTest`
- `` ```yaml `` — **ILLUSTRATIVE**: shows syntax without execution
- `` ```yaml file=filename.spec.yaml `` — **FILE CREATION**: creates temporary files during test execution
- `<!-- yaml specscript -->` — **HIDDEN EXECUTABLE**: runs but not rendered in docs (setup/cleanup)

## Rules for specification documents

1. All `yaml specscript` blocks must be valid, working code
2. Use `localhost:2525` endpoints — `sample-server.spec.yaml` runs automatically during tests
3. Invalid examples must use plain `yaml`, never `yaml specscript`
4. Use sentence case for section titles
5. Multiple commands in one block require `---` separators (YAML duplicate key constraint)
6. Always run `./gradlew specificationTest` before committing
7. Keep main spec documents concise — one or two examples per feature to illustrate the concept
8. Put edge cases, combinations, and thorough coverage in a separate `tests/<Topic> tests.spec.yaml` file next to the
   spec document (e.g., `specification/language/tests/Variables tests.spec.yaml` for `Variables.spec.md`)

## Sample server endpoints (available during tests)

- `POST /greeting` — accepts `name` and `language`, returns formatted greeting
- `GET /items` — returns `[1, 2, 3]`
- `POST /items` — echoes back input as `Fields: ${input}`
- `GET /hello` — returns `"Hello from SpecScript!"`

## File extension rules

- **`.spec.md`**: Executable documentation with `yaml specscript` blocks
- **`.spec.yaml`**: Pure SpecScript YAML scripts
- Never use `yaml file=filename.spec.yaml` as a substitute for executable code in `.spec.md` files
