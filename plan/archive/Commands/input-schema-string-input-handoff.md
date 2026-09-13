# Report: Prompt → JSON Schema, Input parameters removal, and handoff for string input on Input schema

## What shipped this session (on `main`, pushed to origin)

- `daea902` `💫 ⚠️ Prompt uses JSON Schema input, like Input schema`
- `e543556` `TypeScript: Prompt uses JSON Schema input, like Input schema`
- `10c0fde` `⚠️ Remove the deprecated Input parameters command`
- (+ two `Plan` commits for `plan/proposals` and `plan/agent-ideas.md`)

### Prompt now takes a JSON Schema

`Prompt` accepts three forms, all returning to `${output}`:

- string shorthand: `Prompt: What is your name?`
- a single property definition (object without `properties`)
- an object schema (`type: object` + `properties`) → asks each, returns an object

`Prompt` and `Input schema` share **one per-property resolver**. Only the sink differs: `Prompt` → `${output}`; `Input schema` → named vars + `${input}`.

### Keyword model (breaking, JSON-Schema-aligned)

| Concept | Keyword |
|---|---|
| question shown to user | `title` (falls back to `description`, then property name) |
| help text | `description` |
| password masking | `format: password` |
| multiple selection | `type: array` + `items.enum` |
| conditional property | `x-condition` |
| env var source | `x-env` (Input schema only) |
| enum object label / value field | `x-title-property` / `x-value-property` |
| CLI short flag | `x-short-option` (Input schema only) |

Standard JSON Schema keywords bare; SpecScript extensions namespaced with `x-`. Old spellings (`secret`, `select`, `display property`, `value property`, `condition`, `env`, `short option`) are **gone**.

### Resolution order (the shared resolver)

```
1. already-set var   (Input schema only)
2. x-env             (Input schema only)
3. recorded answer
4. interactive?      → ask, with `default` as the pre-filled hint
5. non-interactive   → `default` (raw JSON type preserved — a numeric default stays numeric)
6. otherwise         → throw  (Prompt catches for text → placeholder; a choice always throws)
```

The placeholder for a missing non-interactive text prompt is exactly `[default answer in non-interactive mode]`.

### Input parameters removed

`Input parameters` was deleted in `10c0fde`. Its shared `populateInputVariables` + `conditionValid` moved into `InputSchema.kt` (Kotlin) and stayed in `script-info.ts` (TypeScript). `Script.getScriptInfo`, `McpServer.deriveInputSchema`, `CommandLibrary`, and the TS equivalents no longer reference it.

## Next task: support string (value) input on Input schema

**Important context:** the phrase "Input parameter command" refers to the command just removed. The capability that maps to "string input" is the **value form** that only the old `InputParameters` had. Recover it from git history: `git show 10c0fde^:src/main/kotlin/specscript/commands/scriptinfo/InputParameters.kt`. The relevant handler was:

```kotlin
override fun execute(data: ValueNode, context: ScriptContext): JsonNode? {
    val type = TypeSpecification(data.stringValue())
    val resolvedType = type.resolve(context.types).definition
    if (resolvedType.properties != null) {
        populateInputVariables(context, resolvedType.properties)   // now lives in InputSchema.kt
    } else {
        // TODO handle array and simple types
    }
    return context.getInputVariables()
}
```

So `Input schema: SomeType` should resolve a named type (see `Type` / type-system specs) into its properties and populate input from them. `InputSchema` is currently `ObjectHandler, DelayedResolver` only — the task is to also make it a `ValueHandler`. Decide first (with the user) what "string input" should mean: a named-type reference (as above), or a scalar shorthand — clarify before writing the spec.

## How to do the work (spec-first — this is the SpecScript law)

Full process is in `AGENTS.md` ("Development Process") and the `specscript-specs` skill (`.agents/skills/specscript-specs/SKILL.md`). Order:

1. **Proposal** in `plan/proposals/*.md` (plain Markdown) → **user confirms**. Skip only for trivial changes.
2. **Spec** — draft in `plan/proposals/*.spec.md`, then (after sign-off) move into `specification/`. **The spec IS the test suite**: every ` ```yaml specscript ` block runs during `./gradlew specificationTest`. Captions required (`Code example: ...`). Support table at top; keep it dry (one example per concept, edge cases → `tests/<Topic> tests.spec.yaml`). Recorded answers go in `<!-- answers ... -->` blocks; console assertions in ` ```output ` blocks; the sample HTTP server on `localhost:2525` is up during tests.
3. **Kotlin**, then **TypeScript** (port; plain `TypeScript: ...` commit summary, no `💫`). Both implementations must pass the **same** shared `specification/` suite.

### Key files

- Kotlin: `src/main/kotlin/specscript/commands/scriptinfo/InputSchema.kt` (command + shared `populateInputVariables`), `userinteraction/ParameterDataPrompt.kt` (`resolveValue`, `ask`, dispatch), `userinteraction/Prompt.kt`, `userinteraction/UserPrompt.kt`, `language/types/PropertyDefinition.kt` (`ParameterData`/`PropertySpecification`, `title`/`format`/`items`/`x-*`), `language/types/TypeSpecification.kt` + `type-system` for named types, `commands/CommandLibrary.kt`.
- TypeScript: `typescript/src/commands/script-info.ts`, `commands/prompt.ts` (exports `resolveValue`, `passesCondition`, `question`, `isChoice`), `language/user-prompt.ts`, `commands/register.ts`, `cli.ts`.
- Schema: `specification/commands/core/ParameterData.schema.yaml` (shared), `Input schema.schema.yaml`, `Prompt.schema.yaml`. Spec doc: `specification/commands/core/script-info/Input schema.spec.md` + `tests/Input schema tests.spec.yaml`.

### Build / test

```
./gradlew build                 # Kotlin: unit + specificationTest (must be green)
./gradlew specificationTest --rerun-tasks   # spec test files aren't declared gradle inputs → force rerun after editing them
typescript/node_modules/.bin/vitest run --root typescript   # TS suite (shell `cd` is overridden here; call binaries by path)
java -jar build/libs/specscript-*-full.jar --test <file>    # run one spec/test file directly to see diffs (build with `./gradlew fullJar`)
```

## Gotchas worth knowing

- **`default` returns its raw JSON type.** `default: 2` yields integer `2`, not `"2"`. Tests must match the type. This is consistent between Prompt and Input schema.
- **Kotlin's test runner leaks recorded answers across test cases** in a file (shared context); the TypeScript runner isolates them. Do **not** write tests that rely on an answer set in an earlier case — make each test self-contained. (Recorded in `plan/agent-ideas.md`.)
- **`Prompt` and `Input schema` are `DelayedResolver`s** — the engine does not pre-resolve `${...}`; they resolve variables themselves (Prompt resolves the whole def; Input schema resolves only `x-condition`, matching the raw-enum/default behavior).
- **`title` vs `description`:** the interactive question uses `title` → `description` → name; CLI `--help` uses `description` → `title`. MCP tool `inputSchema` is passed through raw (`x-`/`format`/`title` survive).
- **Commit rules (`AGENTS.md`):** work on `main`, no branches. `--author="claude-<model> <...@specscript.dev>"`, never touch git config. Breaking → `⚠️` (with `💫` only if it's also a new feature). Don't commit `.claude/settings.local.json`.
```
