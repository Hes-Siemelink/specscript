# Proposal: Align `Prompt` with `Input schema` (shared JSON Schema property model)

## Problem

`Prompt`, `Prompt object`, and `Input schema` all describe user input using the same
`PropertyDefinition`/`ParameterData` vocabulary (`description`, `default`, `type`, `enum`,
`secret`, `select`, `display property`, `value property`, `condition`, `env`, `short option`). Yet they run through *
*two divergent resolution code paths** that behave differently:

- `Prompt` / `Prompt object` → `UserPrompt.prompt`/`select` (via `ParameterDataPrompt.kt`)
- `Input schema` / `Input parameters` → `InputParameters.populateInputVariables`

The two paths disagree on real, observable behavior:

| Behavior                            | `Prompt` / `Prompt object`                            | `Input schema`                                                                |
|-------------------------------------|-------------------------------------------------------|-------------------------------------------------------------------------------|
| Resolution order                    | recorded answer → default → interactive → placeholder | already-set var → `env` → default → recorded answer → interactive → **error** |
| `env` support                       | **no**                                                | yes                                                                           |
| Non-interactive, no value           | returns `"[default answer in non-interactive mode]"`  | throws `MissingInputException`                                                |
| Skip if already provided            | no                                                    | yes (checks `${input}`)                                                       |
| Result exposure                     | `${output}` only                                      | named top-level var **and** `${input}.<name>`                                 |
| Default vs recorded answer priority | recorded answer wins                                  | **default wins**                                                              |

This duplication is the root issue: the same property definition means two different things depending on which command
reads it. The goal is one property model, one resolution engine, shared by both commands, so they behave identically per
property.

## Goal

1. `Prompt`'s object form **is a single JSON Schema property** — parsed and resolved exactly like one entry under
   `Input schema`'s `properties`.
2. `Input schema` and `Prompt` share **one** per-property resolution function (order, `env`, interactive display,
   non-interactive behavior all identical).
3. Both commands read **the same JSON Schema**, using standard keywords where they exist and an
   `x-` prefix for SpecScript-specific ones. Only the result sink differs: `Prompt` → `${output}`,
   `Input schema` → named vars + `${input}`.

## Resolved design (decisions confirmed)

### 1. One property, one resolver

Extract a single per-property resolution function. Steps 1–2 are `Input schema`-only (`Prompt` has
no external input source); steps 3–6 are shared:

```
1. already-set var   (Input schema)  → use it
2. x-env             (Input schema)  → use env value
3. recorded answer                   → use it (simulates the user)
4. interactive?                      → ask, with `default` as the pre-filled hint
5. non-interactive                   → `default`, if any
6. otherwise                         → throw MissingInputException
```

Key points:

- **`env` overrides a recorded answer** (step 2 before 3). A wrong env var in a test is the
  invoker's mistake, not a spec concern.
- **Recorded answer overrides `default`** (step 3 before 5). `Input schema` currently lets `default`
  win; it changes to match.
- **`default` never short-circuits an interactive prompt.** It is a *hint* when interactive (step 4)
  and the *fallback value* only when non-interactive (step 5). This changes today's
  `UserPrompt.prompt`, which returns the default outright even in interactive mode.
- **The resolver is strict — step 6 always throws.** `Prompt` wraps it: for a *text* property it
  catches and substitutes the placeholder (`UserPrompt.kt` line 57); for a *choice/array* it lets
  the error stand (no safe default). `Input schema` does not wrap → errors as today. Leniency is a
  thin `Prompt`-only shell over one shared, strict code path.
- **`x-env` and already-set are `Input schema`-only.** `Prompt` shares only steps 3–6.

### 2. `Prompt` accepts a JSON Schema (same shape as `Input schema`)

Three input forms, all funnelling through the shared resolver:

```yaml
# String — shorthand for a single string property
Prompt: What is your name?           # ⇔  { title: What is your name?, type: string }

# Single property node — scalar ${output}
Prompt:
  title: What is your favorite color?
  enum: [ Red, Green, Blue ]

# Object schema — object ${output}
Prompt:
  type: object
  properties:
    firstName: { title: First name }
    lastName: { title: Last name }
# ${output} = { "firstName": "...", "lastName": "..." }
```

Result always lands in `${output}` (capture with `As` as usual). **Disambiguation rule:** a
`properties` key (object schema) ⇒ multi-property object output; otherwise ⇒ single property, scalar output.
`type: object` is *not* required on `Prompt` (the `properties` key is the signal);
`Input schema` keeps requiring `type: object` + `properties`.

### 2a. JSON Schema keyword mapping (breaking)

Property keywords are aligned to JSON Schema. Standard keywords are used verbatim; SpecScript
extensions are `x-`-prefixed. **This is a hard break — old spellings are removed, no aliases.**

| Concept                | Old (removed)            | New                          | Applies to        |
|------------------------|--------------------------|------------------------------|-------------------|
| Question text          | `description`            | `title` (`description` = help)| both              |
| Password masking       | `secret: true`           | `format: password`           | both              |
| Multiple selection     | `select: multiple`       | `type: array` + `items.enum` | both              |
| Conditional property   | `condition`              | `x-condition`                | both              |
| Display field for enum | `display property`       | `x-display-property`         | both              |
| Value field for enum   | `value property`         | `x-value-property`           | both              |
| Env var source         | `env`                    | `x-env`                      | Input schema only |
| CLI short flag         | `short option`           | `x-short-option`             | Input schema only |

- `title` is the prompt question; `description` becomes optional help text. Message resolution:
  `title` → `description` → property name.
- `type: string` is the default and can be omitted.
- `format` is extensible (`email`, `date`, … later); only `password` is functional now, the rest
  informational like `type`.
- The shared keywords apply to **both** `Prompt` and `Input schema`, and to the MCP tool
  `inputSchema` derivation. `x-env` and `x-short-option` are meaningful only for `Input schema`
  (`Prompt` has no external input source).

### 3. `Prompt object` falls away

`Prompt` now covers the multi-property case via the `properties` form, so `Prompt object` is absorbed. Its spec and
command are removed; existing `Prompt object` scripts migrate to the
`properties` form.

### 4. `Input schema` = same schema, different sink

`type: object` + `properties` + `required`, each property resolved via the **same** shared resolver as `Prompt`. The
only difference from `Prompt` is where results go: `Input schema`
populates named top-level variables and `${input}`; `Prompt` returns to `${output}`. There is no
`name:`/named-exposure feature on `Prompt` — that was dropped.

## Out of scope

- Broadening the JSON Schema subset (`pattern`, `minimum`, nested objects, etc.) — unchanged.
- `Input parameters` (legacy) — left as-is; it already shares the `InputParameters` path.

## Files touched (implementation, after spec sign-off)

- `specification/commands/core/user-interaction/Prompt.spec.md` (fold in `plan/proposals/Prompt.spec.md`; add
  `properties` form)
- `specification/commands/core/user-interaction/Prompt object.spec.md` + schema + tests — **removed**
- `specification/commands/core/script-info/Input schema.spec.md` (note shared behavior; recorded-answer-over-default)
- `src/.../userinteraction/Prompt.kt`, `ParameterDataPrompt.kt`, `UserPrompt.kt`, `PromptObject.kt` (removed)
- `src/.../scriptinfo/InputParameters.kt`, `InputSchema.kt` (extract shared resolver)
- `src/.../language/types/PropertyDefinition.kt` / `ParameterData` — rename fields to JSON Schema
  keywords (`title`, `format`, `x-condition`, `x-env`, `x-display-property`, `x-value-property`,
  `x-short-option`), add `items`, drop `secret`/`select`
- Schemas: `Prompt.schema.yaml`, `ParameterData.schema.yaml` (new keywords + `x-` + `type: array`/`items`),
  `Input schema.schema.yaml`, `CommandLibrary.kt` (drop `Prompt object`)
- Commit is **breaking**: `💫 ⚠️` per AGENTS.md.
