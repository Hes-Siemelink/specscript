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
- `` ```yaml temp-file=filename.spec.yaml `` — **FILE CREATION**: creates temporary files during test execution
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
9. **Always introduce code blocks with text.** Markdown viewers hide the ```` ``` ```` header, so the reader cannot
   see block type, language, or `file=` annotations. Every code block needs a preceding sentence that provides
   context — what the block is, what file it creates, or what it demonstrates.

## Writing style

Spec documents are written spec-first — they define behavior before implementation exists. This drives a minimalist
style: precise, executable, and cheap to change.

- **One executable example per concept**, two maximum per section. The example IS the test.
- **No motivational prose.** No "this is useful for", "previously you had to", or "in CI/CD environments" paragraphs.
- **No illustrative non-executable examples.** If it can't run as a test, it probably doesn't belong in the spec.
- **Don't explain what the reader can infer** from the example or from a stated resolution order.
- **Prefer a summary table** over multiple explanatory paragraphs.
- **Add new features as sections in existing spec files** rather than creating new spec files.
- **Keep prose to one or two sentences** introducing the feature and its core rule. Then show the example.
- **Edge cases go in `tests/` files**, not in the main spec document.

This style is deliberately concise, but should build up the functionality from simple to complex within the document.
More elaborate tutorial-style guides are a separate concern (see TODO.md).

## Sample server endpoints (available during tests)

- `POST /greeting` — accepts `name` and `language`, returns formatted greeting
- `GET /items` — returns `[1, 2, 3]`
- `POST /items` — echoes back input as `Fields: ${input}`
- `GET /hello` — returns `"Hello from SpecScript!"`

## File extension rules

- **`.spec.md`**: Executable documentation with `yaml specscript` blocks
- **`.spec.yaml`**: Pure SpecScript YAML scripts
- Never use `yaml temp-file=filename.spec.yaml` as a substitute for executable code in `.spec.md` files

## Spec document hygiene

Lessons from implementing a second (TypeScript) implementation against the spec:

- **Avoid asserting on exact formatted output** when the intent is to test behavior, not formatting. Use pattern
  matching or partial assertions. Exact output assertions make formatting quirks de-facto spec — a second implementation
  is forced to replicate bugs.
- **YAML output format varies between libraries** (indentation, quoting style, flow vs. block). Prefer comparing parsed
  structures (semantic equality) over formatted YAML strings where possible.
- **Watch for cross-level contamination** in spec files. If a Level 0–1 spec file uses `temp-file=` blocks or `cli`
  blocks (Level 3+ features), it forces test runners to handle partial failures. Keep spec sections within their level,
  or move cross-level sections to separate files.
