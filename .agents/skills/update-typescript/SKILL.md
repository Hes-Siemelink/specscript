---
name: update-typescript
description: Updates the TypeScript implementation of SpecScript after changes to the specification and the Kotlin reference implementation. Introduces the SpecScript language, the project layout, and the spec-first workflow so a fresh session can port changes without reading the whole specification. Use when the user says "update typescript", wants a feature or fix ported to TypeScript, or asks to mirror a spec/Kotlin change in the TS implementation.
compatibility: Requires the specscript repo and the typescript/ node project.
metadata:
  author: specscript
  version: "1.0"
---

## Overview

SpecScript is a scripting language where YAML is both the data format and the code format: `.spec.yaml` files are maps
whose keys are **commands** and whose values are the command inputs, executed top to bottom.

The repo has three layers that must stay in sync:

1. **specification/** — the language specification. Executable documentation: every code example is a test.
2. **src/main/kotlin/** — the Kotlin reference implementation (the source of truth for behavior).
3. **typescript/** — an independent TypeScript implementation that must pass the same spec tests.

Work usually lands in spec + Kotlin first. Your job is often to bring TypeScript up to par: "update typescript". This
skill gives you the whole SpecScript mental model so you do not need to read the specification end to end. The
acceptance criteria are always the **specification tests**, which run the document examples against the implementation.

## SpecScript in one page

### Scripts and YAML

```yaml
Script info: Fetch items and print names

Print: Hello World

GET: /api/items
As: ${items}

For each:
  ${item} in: ${items}
  Print: ${item.name}
```

- A script is a sequence of command keys. Order matters.
- **`---`** separates "documents" and is required whenever the same key would repeat (YAML silently drops duplicate
  keys — this is the single biggest pitfall). When in doubt, add `---`.
- Dictionary keys are command names (case-insensitive, capitalized by convention).

### Command input forms

Every command has a "Value | List | Object" support matrix (shown in each command's spec). The engine auto-iterates a
list input over commands that don't handle lists natively.

### Variables, eval, and control flow

```yaml
${greeting}: Hello World                # assignment
Print: ${greeting}                      # interpolation anywhere in a string

GET: /api/users
As: ${users}                            # capture last output into a variable
Print: ${users[0].name}                 # nested access

If:
  item: ${status}
  equals: active
  then:
    Output: Running

When: ...                               # multi-branch conditional (first match wins)
Repeat:
  ...  until: ...                 # loop until condition
Do: # group repeated commands (list of commands)
  - Print: a
  - Print: b
```

- `${output}` is the result of the last command. Built-ins: `${input}`, `${env.VAR}`, `${SCRIPT_HOME}`, `${PWD}`,
  `${SCRIPT_TEMP_DIR}`.
- **Eval syntax**: prefix a command with `/` inside a data structure to run it inline, e.g.
  `Output: {count: /Size: ${items}}`. Avoids intermediate variables.
- `For each` with a nested `Output` collects the results; `As:` captures the transformed list.
- Filters use `For each` + `If` + `Output` (there is no `Filter`). Sizes use the `Size` command (no `.size`/`.length`).

### HTTP

```yaml
Http request defaults: # base url + headers for subsequent requests
  url: https://api.example.com

GET: /api/items
POST:
  url: /items
  body:
    name: New Item

Http session: # named session provides defaults; current session wins
  name: backend
  url: https://api.example.com
```

- `GET`, `POST`, `PUT`, `PATCH`, `DELETE` share the same parameter model (`HttpParameters.schema.yaml`): `url`, `path`,
  `body`, `headers`, `cookies`, `username`, `password`, `save as`, `session`.
- Sessions hold defaults for all subsequent requests; the current session is used unless `session:` is given. A session
  name passed as the **value** of `Http session` switches the current session (`Http session: name`); an empty string
  leaves it unchanged.
- Defaults from a session are stored in the session's data object when opened; the session name is auto-generated when
  omitted.

### Scripts as commands, packages, and connections

- Any `.spec.yaml` file becomes a command: `features/list.spec.yaml` → `spec features list`. Directory metadata lives in
  a `specscript-config.yaml` (description, `imports:`, `connections:`).
- `Input schema:` defines CLI flags (an object form with properties); values are injected as `input`.
- `Connect to:` activates a named set of defaults.

### Spec docs are tests

`.spec.md` files in `specification/` contain code blocks tagged `yaml specscript` (plus hidden `yaml temp-file=` and
`shell` blocks). **These blocks execute during the test suite.** Both implementations must run them and produce the same
results — that is what keeps the two implementations honest and the docs truthful. Edge cases live in separate
`tests/<Topic> tests.spec.yaml` files next to the spec (run with `Before/After`, `Tests`, `Code example`).

## Repository layout

```
specification/                    language spec, command reference, executable docs, sample server, code-examples
  language/                       core syntax: scripts, variables, eval, packages, conditions
  commands/core/                  command reference, one dir per group (http, control-flow, testing, ...)
  code-examples/sample-server/    start.spec.yaml — mock server on localhost:2525 started by the tests
src/main/kotlin/specscript/       Kotlin reference implementation
  language/                       engine: CommandExecution, Sessions, ScriptContext, conditions, eval, variables
  commands/                       command implementations grouped by area (http/, mcp/, ...)
  schema/                         JSON Schema for command YAML shapes
typescript/                       the TypeScript implementation
  src/language/                   engine (see below)
  src/commands/                   command implementations, one kebab-case file per command group
  src/markdown/                   parses .spec.md files into executable tests
  test/spec-runner.test.ts        runs all specification files against TS; SKIP_TESTS lists TS-unimplemented features
samples/                          example scripts
plan/                             proposals, draft specs, reports, ideas (spec-first artifacts)
```

### Where things live in TypeScript

| Kotlin                                 | TypeScript                          | What it is                                       |
|----------------------------------------|-------------------------------------|--------------------------------------------------|
| `language/Sessions.kt`                 | `src/language/sessions.ts`          | named sessions shared by HTTP and MCP registries |
| `language/CommandExecution.kt`         | `src/language/command-execution.ts` | resolve→dispatch pipeline, error wrapping        |
| `language/ScriptContext.kt`            | `src/language/context.ts`           | execution context: variables, session, dirs      |
| `commands/http/HttpSession.kt`         | `src/commands/http-sessions.ts`     | `Http session` / `Http close session`            |
| `commands/http/HttpClient.kt`          | `src/commands/http-client.ts`       | shared request processing for HTTP commands      |
| `commands/http/Get.kt`, `Post.kt`, ... | `src/commands/http.ts`              | the HTTP verb commands                           |
| `commands/mcp/...`                     | `src/commands/mcp-server.ts`        | MCP server, tools, resources, prompts, sessions  |
| `commands/control-flow/...`            | `src/commands/control-flow.ts`      | If / When / For each / Repeat / Do               |
| `commands/data-manipulation/...`       | `src/commands/data-manipulation.ts` | Size / Sort / Find / Add to / Fields / ...       |

TS engine files you will touch: `language/types.ts` (JsonValue + error classes), `language/command-handler.ts`
(CommandHandler interface + registry), `commands/register.ts` (`registerAllCommands()`).

## Workflow: spec → Kotlin → TypeScript

The order is fixed: **specification first, then Kotlin, then TypeScript.** The spec defines behavior; Kotlin is the
reference for exact semantics; TypeScript mirrors Kotlin. TypeScript only ever *catches up* — it never leads.

For the full proposal → spec → implement → report → commit loop, load the `specscript-development-process` skill. For
writing spec documents, load `specscript-specs`. For writing SpecScript scripts, load `specscript-coding`.

## Porting a change to TypeScript

### 1. Understand the change before touching code

- `git status` and `git diff` to see what changed in `specification/` and `src/main/kotlin/`.
- **Read the changed spec files.** The executable examples ARE the expected behavior — the same examples will run
  against TypeScript. Note new or renamed commands, new input forms (value/object/list), and changed semantics.
- Check whether the sample server changed (`specification/code-examples/sample-server/start.spec.yaml`): endpoints there
  are shared by every HTTP test across both implementations.
- **Read the changed Kotlin files.** TypeScript mirrors Kotlin 1:1 including message strings, error types, and edge
  cases. Do not guess behavior from memory — match the Kotlin.

### 2. Locate the TypeScript counterpart

- Commands map by name: Kotlin `commands/http/HttpSession.kt` → `src/commands/http-sessions.ts`; Kotlin
  `commands/control-flow/When.kt` → `src/commands/control-flow.ts`. Use the mirroring table above.
- Shared machinery maps directly: Kotlin `language/Sessions.kt` → `src/language/sessions.ts`, and so on.
- If there is no obvious TS file yet, the feature is not ported — decide (with the user) whether it belongs in an
  existing file or merits a new one.

### 3. Mirror the Kotlin semantics

Preserve Kotlin behavior exactly. Concretely:

- **Input forms**: Kotlin's `ValueHandler` ↔ TS `isString(data)` branch; `ObjectHandler` ↔ `isObject(data)` branch;
  unordered handling ↔ `CommandFormatError` on input that matches neither. Commands that natively consume lists set
  `handlesLists`; everything else gets auto-iterated by `runCommand`.
- **Error semantics**: throw `SpecScriptCommandError` for user-facing command errors (same message as Kotlin). A Kotlin
  `IllegalArgumentException` thrown mid-command becomes an internal error — mirror it by throwing a plain `Error`; the
  TS pipeline wraps unexpected errors in `SpecScriptInternalError` just like Kotlin.
- **Context invariants**: `context.clone()` shares the `session` map by reference (not cloned) in both implementations;
  session `SessionRegistry` instances are module-level singletons that track the current session by name. Behavior must
  not depend on a per-test fresh registry.
- Return the same data shapes as Kotlin (including the exact extra field, e.g. an injected `name`), because spec
  examples assert on them.
- Match "live" Kotlin without fixing it: if Kotlin has an oddity, mirror it and note it in `plan/agent-ideas.md` as a
  one-liner. Do not redesign while porting.

### 4. Test with vitest

```sh
cd typescript
npx vitest run            # full specification suite against TS (a few seconds)
npx vitest run -t <text>  # filter to matching test names
```

- The changed spec examples must pass. The full suite is the gate — do not commit ported code on a subset alone.
- `test/spec-runner.test.ts` has a `SKIP_TESTS` set for features TypeScript does not implement yet (e.g. `Validate
  schema`). If your change lands in an area on that list, verify whether the skip can now be removed.
- `npx tsc --noEmit` has pre-existing unrelated errors on main; if you typecheck, limit scope to the files you changed
  (e.g. via `grep 'your-file'`).

### 5. Report and close

- Summarize: files changed, test results (before/after counts), any spec gaps or behavior notes worth surfacing.
- The user reviews the diff and commits. See commit rules below.

## Porting rules and gotchas

- **TypeScript is a faithful mirror, not a redesign.** Follow existing TS patterns; never invent new architecture
  without asking. Improvement ideas go into `plan/agent-ideas.md` as one-liners.
- **Don't touch Kotlin or the spec while porting** — the user owns those; you own the TS side. If the spec is genuinely
  wrong (e.g. an example won't pass either implementation), raise it with the user instead of "fixing" it.
- When renaming or adding sample-server endpoints, update `sample-server/start.spec.yaml` AND every spec/tests file
  referencing the old path — the tests hit the live server.
- Avoid making TS pass by weakening assertions. Match the intent of the spec example.
- Cross-implementation divergence notes are captured in `plan/proposals/language-designer-lessons-learned.md` — read it
  for known traps (error type names, auto-list null handling, output-format assertions).

## Commit conventions

- **Always ask for confirmation before committing.** The user reviews and pushes.
- Catch-up ports of an already-released spec feature: plain summary, e.g. `TypeScript: Changing sessions by name`.
- Work that includes the spec change + Kotlin + TS in one land: normal feature rules — `💫` for spec-level new features.
- Summary ≤ 70 characters; each body point its own `-m`; author
  `--author="<model> <model>@specscript.dev"`; add `🤖 Generated by <model>` in the body.
- Plan files (`plan/proposals`, `plan/reports`) are committed in a separate `Plan` commit.
- Use the exact code words from AGENTS.md for non-feature commits (`Bug fix`, `Refactoring`, `Documentation`,
  `Code Cleanup`, `AI Context`).

## When to use

- User says "update typescript" or "port this to TypeScript" after spec + Kotlin changes.
- A spec feature or fix is missing or behaving differently in the TS implementation.
- You need to bring the TS implementation back in line with the specification test suite.