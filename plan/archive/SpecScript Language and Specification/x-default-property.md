# Proposal: `x-default-property` — a scalar shorthand for one object property

## Status

Supersedes [input-schema-supports-type-string.md](./input-schema-supports-type-string.md). That proposal's headline
(`Input schema: { type: string }` → bare-string `${input}`) is **deferred**: it breaks the invariant that `${input}` is
an object (`getInputVariables()` in `ScriptContext.kt:33` casts to `ObjectNode`), which ripples into MCP (string schemas
that clients choke on) and the CLI. This proposal delivers the ergonomics that proposal was chasing — pass a script or
command a plain value — while keeping the object invariant intact.

Decisions locked with the user:

- **Name:** `x-default-property`. Signals "there is some fancy footwork here," is type-agnostic, uses JSON Schema's
  "property" vocabulary, and avoids colliding with JSON Schema `default` (the default *value*).
- **Type declaration:** the schema declares a union `type: [object, string]` (see §2). Free to change existing schema
  style across the board.
- **CLI:** `--<shorthand-property>` only — already works, no new code (see §3).
- **Docs:** add a "Shorthand property" row to the support table; keep it hand-written (do not generate from schema).
- **Validation:** a malformed `x-default-property` fails at load time (see Resolved).

## 1. The idea

A schema declares an object shape and names one **scalar** property as the shorthand. When the command/script is called
with a bare scalar instead of an object, the scalar fills that property.

```yaml
type: [object, string]
x-default-property: name
properties:
  name:
    type: string
    default: Stranger
  email:
    type: string
```

```
Greet user: Hes        ==  Greet user: { name: Hes }        →  ${name} = "Hes"
GET: http://host/items ==  GET: { url: http://host/items }
```

`${input}` (and any object the command receives) stays an object in both cases. Nothing downstream — MCP, plumbing,
`${input}.x` — has to learn about scalars.

### Constraints on the shorthand property

- Must name a property that exists in `properties`.
- Must be a **scalar** type (string / number / boolean / integer). Lifting a scalar into an object-typed property is
  meaningless. Reject at load time (see Open Questions).

## 2. Schema convention: `type: [object, string]` + `x-default-property`

The schemas already express the object-or-value union, but via `oneOf`, which cannot say *which* property the scalar
maps to:

```yaml
# GET.schema.yaml today — union, but no target property
oneOf:
  - type: string
  - $ref: "HttpParameters.schema.yaml"
```

New convention for shorthand commands:

```yaml
# GET.schema.yaml — union as a type array + the missing metadata
$schema: https://json-schema.org/draft/2020-12/schema
type: [object, string]
x-default-property: url
properties:
  url:   { type: string }
  path:  { type: string }
  # ...
```

`type: [object, string]` is valid JSON Schema and MCP clients read type arrays fine. Implementation detail:
`x-default-property` lives wherever the property set lives — for `GET`, that means on the `HttpParameters` property set
it references, not on a bare `oneOf` wrapper.

## 3. The call paths

### In-process: `Run`

`Run` already passes `input` through as a raw node (`Run.kt` → `runScriptFile(file, input, context)`), so no change to
`Run` itself:

```yaml
Run:
  script: greet-user.cli.md
  input: Hes
```

### In-process: generated ("magic") command

```yaml
Greet user: Hes
```

Routes through `SpecScriptFile` as a command handler. `SpecScriptFile` is `ObjectHandler` only today; it gains a
`ValueHandler` so a scalar arg dispatches correctly (`CommandExecution.kt:85`).

### CLI — named flag (recommended), not positional

The shorthand property **already works on the CLI today** with no new code: for `x-default-property: name`,
`spec greet-user --name Hes` sets `${name}` (via `toParameterMap` → merged input). So the only open question is whether
to also support a *name-agnostic* form.

Decision: **rely solely on `--<shorthand-property>` — the simplest thing that already works.** No positional, no generic
`--input` alias.

|             | Positional (`… script Hes`)                                                                                 | `--<shorthand-property>` (`… script --name Hes`) |
|-------------|-------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| Parser work | High — rewrite the `splitArguments` state machine + leaf-detection                                          | **None — already works today**                   |
| Ambiguity   | Footgun — in `spec dir dir script Hes`, a value colliding with a command name is eaten by greedy navigation | Zero — navigation tokens bare, value flagged     |
| Readability | reader can't see where the tree ends and the arg begins                                                     | `--name` names the property                      |

So the CLI needs **no work at all**: `spec greet-user --name Hes` already maps to `${name}` via `toParameterMap` →
merged input. Positional and the generic `--input` alias are both dropped.

### MCP

No special handling, no client breakage. `deriveInputSchema` (`McpServer.kt:235`) passes the object schema through;
clients call with `{ name: "Hes" }` and ignore the `x-default-property` hint. This is the payoff of the object route
over bare `type: string`.

## 4. Commands that gain a shorthand property

Every handler that is **both** `ObjectHandler` and `ValueHandler` where the value form is genuinely one property lifted:

| Command                   | Scalar shorthand            | `x-default-property`             |
|---------------------------|-----------------------------|----------------------------------|
| `GET` / `POST` / `DELETE` | `GET: http://host/items`    | `url`                            |
| `Run`                     | `Run: script.spec.yaml`     | `script`                         |
| `Cli`                     | `Cli: git status`           | `command`                        |
| `Shell`                   | `Shell: ls -la`             | `command`                        |
| `Read file`               | `Read file: data.yaml`      | `file`                           |
| `Write file`              | `Write file: out.txt`       | `file`                           |
| `Temp file`               | `Temp file: contents`       | `content`                        |
| `Error`                   | `Error: Boom`               | `message`                        |
| `Expected error`          | `Expected error: Boom`      | `message` (confirm)              |
| `Prompt`                  | `Prompt: What's your name?` | `description` (special resolver) |
| *script files*            | `Greet user: Hes`           | *(author-declared)*              |

**Deliberately excluded** — `Object+Value`, but the value form is *not* a single-property lift, so the annotation would
be a lie:

- **`Size`** — polymorphic; the value *is* the data being measured.
- **`Fields`** — value form ignores its argument and reads `${output}`.
- **`Http request defaults` / `SQLite defaults`** — value form is a **getter**; object form is a **setter**. Different
  semantics, not shorthand.

### Documentation: the support table

The `| Input | Supported |` table is hand-written illustrative markdown (not code-driven). It stays hand-written — we do
**not** generate it from the schema (more complexity than the drift risk warrants). The schema reference stays too (JSON
Schema is authoritative but unreadable to newcomers). We add a **Shorthand property** row:

```markdown
| Input     | Supported    |
|-----------|--------------|
| Value     | yes          |
| List      | auto-iterate |
| Object    | yes          |
| Default property | `url`        |
```

Read together: the *Value* row says "a bare value is accepted"; the *Shorthand* row says "it fills the `url` property."

## 5. Implementation sketch

One wrapping helper, one home: when a script/command is invoked with a scalar, consult the schema's
`x-default-property` and produce `{ <property>: <scalar> }` **before** it becomes `INPUT_VARIABLE`, so
`getInputVariables()` never sees a non-object.

- **`SpecScriptFile`** (`files/SpecScriptFile.kt`): implement `ValueHandler`. `execute(ValueNode)` reads the script's
  shorthand property (same lookup as `McpServer.deriveInputSchema`), wraps the scalar, and runs. Covers `Run` and the
  generated command (both reach `runCommand(SpecScriptFile(file), input, context)`).
- **`InputSchema`** (`commands/scriptinfo/InputSchema.kt`): parse + validate `x-default-property`; expose the name.
  Runtime population is unchanged — input is already an object by the time `populateInputVariables` runs.
- **Built-in shorthand commands**: schema/doc change only (`x-default-property` + `type: [object, string]` + table row).
  They already hand-implement `ValueHandler`; no runtime change.
- **CLI**: no change. `--<shorthand-property>` already works via `toParameterMap`.
- Kotlin first, then TypeScript port (`typescript/src/commands/script-info.ts`, `cli.ts`); both green against the same
  `specification/` suite.

## Out of scope

- **Bare-scalar `${input}`** (`type: string` at the top level → `${input}` is a string). Deferred; revisit only if the
  `Prompt` symmetry is judged worth breaking the object invariant.
- **CLI positional arguments** and a **generic `--input` alias.** Rejected in §3 — `--<shorthand-property>` already
  covers the CLI with zero new code.
- **Shell pipe** (`echo Hes | spec greet-user`). Untestable in the spec harness (no shell in the ` ```cli ` directive).
- **A formal, executed input-support block** replacing the table. Separate doc-tooling task.

## Resolved

- **Validation timing:** fail at **load** time. A bad `x-default-property` (missing property / non-scalar) is validated
  whenever the schema is processed, so a typo'd annotation blows up on *every* invocation — not only the scalar-form
  call. It is a spec error, not a recoverable condition, so it fails as early as possible. ✓
- `Expected error`'s shorthand property is `message`. ✓
- CLI uses `--<shorthand-property>` only; no positional, no generic `--input` alias. ✓
