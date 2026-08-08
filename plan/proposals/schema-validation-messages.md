# Proposal: Custom schema validation error messages via the `message` keyword

## Status

Schema validation was re-enabled for the Kotlin implementation (commit `614e895`, "Reenabled Schema validation for Kotlin
implementation"). Validation errors are currently the raw networknt output, which mixes cryptic formal messages with the
domain problem. This proposal adds human-readable `message:` blocks to the command schemas — but only where the default
output is genuinely cryptic.

## 1. Mechanism

networknt supports custom error messages when `errorMessageKeyword` is configured and the keyword is registered as a
non-validation keyword:

- `JsonSchemas.kt` — custom draft-2020-12 dialect registers `message` as `NonValidationKeyword`; `SchemaRegistryConfig`
  sets `errorMessageKeyword("message")`. Already implemented and verified (`When.schema.yaml`).
- The `message:` block lives at the same level as the keyword it overrides:
  ```yaml
  type: object
  message:
    oneOf: "A condition must use exactly one of 'item', 'all', 'any', 'not' or 'empty'."
  ```
- Messages go through `java.text.MessageFormat`, so a literal single quote must be written `''`.
- The custom message is the first error reported for that keyword; the formal messages follow. Every custom message ends
  with a period — that period is the boundary between the domain-specific message and the JSON Schema formal messaging
  that follows.

## 2. Where it pays off (and where it does not)

Driven by comparing proposed texts against real default output:

| Keyword | Default output | Verdict |
|---|---|---|
| `oneOf` | "must be valid to one and only one schema, but 0 are valid" + all branch noise | **cryptic — worth messaging** |
| `pattern` | "string ... does not match pattern ..." (regex in the error) | **cryptic — worth messaging** |
| `enum` | "must be equal to one of the allowed values" (no hint of the values) | **cryptic — worth messaging** |
| `additionalProperties` | "property 'x' is not allowed" | already names the offending property; low value |
| `required` | "required property 'x' not found" | already readable; low value |
| `type` | "string found, object expected" | already readable; low value |

So: spend effort on `oneOf` / `pattern` / `enum`. De-scope `required` and `type`.

## 3. Phase 1 — messages to add

Inspired by existing Kotlin `CommandFormatException` messages where available.

| Schema | Keyword | Proposed message | Inspiration (Kotlin) |
|---|---|---|---|
| `Conditions.schema.yaml` (`ConditionBase`) | `oneOf` | "A condition must be exactly one of 'item' with 'equals' or 'in', 'all', 'any', 'not', or 'empty'." | `Conditions.kt:125` |
| `Read file.schema.yaml` | `oneOf` | "Read file requires a path string, or an object with a 'file' or 'resource'." | `ReadFile.kt:41` "Expected either 'file' or 'resource' property." |
| `Run.schema.yaml` | `oneOf` | "Run requires a command string, or an object with 'script', 'file', 'cd' or 'input'." | `Run.kt:43` "Expected 'script' or 'file' property" |
| `Shell.schema.yaml` | `oneOf` | "Shell requires either a 'command' or 'resource'." | `Shell.kt:36` "Specify shell command in either 'command' or 'resource' property" |
| `Write file.schema.yaml` | `oneOf` | "Write file requires a content value, or an object with 'file' and 'content'." | `WriteFile.kt:18` |
| `Temp file.schema.yaml` | `oneOf` | "Temp file requires a content value, or an object with 'name', 'resolve' and 'content'." | `WriteFile.kt:18` |
| `Repeat.schema.yaml` (`until`) | `oneOf` | "The 'until' condition must be a condition object, or a string, boolean, number or array." | `Repeat.kt:12` "Repeat needs 'until'" |
| `Add to.schema.yaml` | patternProperties `oneOf`/`type` | "Add to entries must use ${var} variable syntax." | `AddTo.kt:14` "Entries should be in ${..} variable syntax." |
| `As.schema.yaml` | `pattern` | "As requires a variable reference in ${var} syntax." | `As.kt:11` |
| `HttpParameters.schema.yaml` | `enum` (`method`) | "HTTP method must be one of GET, POST, PATCH, PUT, DELETE." | — |
| `Json patch.schema.yaml` (`items.op`) | `enum` | "Json patch 'op' must be one of add, remove, replace, move, copy, test." | — |

`When.schema.yaml` (`items` `oneOf`) is already done and verified.

### Schemas that inherit messages transitively via `$ref` (no edits)

`Assert that` → `Conditions`; `If` condition part → `Conditions`; `When` items → `Conditions`; `Prompt`/`Input schema`
parameter definitions → `ParameterData.schema.yaml`.

## 4. Explicitly de-scoped

- **`required`** — the default "required property 'x' not found" is already readable. All candidates dropped:
  `Assert equals`, `Cli`, `Temp file`, `Delete/Set default credentials`, `Create credentials`, `Json patch` (`patch`,
  `op`/`path`), `Replace`, `Find`, `Sort`.
- **`type`** — the default "string found, object expected" is already readable. Candidates dropped: `Conditions`,
  `Repeat`, `ValidateSchema`, `ValidateType`, `Input schema`, `Prompt`/`ParameterData` type branches.
- **`ValidateSchema` / `ValidateType` / `Input schema`** — schemas in flux / not wired to a live command name; skip.
- **`For each`** — patternProperties value `oneOf`; low usage friction, verify separately if wanted.
- **`additionalProperties`** — default already names the property; skip.

## 5. Verification

- `./gradlew specificationTest` — all 541 tests stay green (no `Unknown keyword message` WARN, no message-text
  assertions broken).
- Manual check per schema: run a failing script and confirm the custom message appears first, ending with a period,
  followed by the formal messages.
- Verify the `patternProperties`-level message placement works for `Add to` (the only non-trivial placement).

## 6. Open questions

1. `Add to` — does a `message:` block placed inside the `patternProperties` value schema get picked up? Fallback:
   drop the message or accept the default.
2. Wording style: command-name prefix with "requires" ("Read file requires a path string...").

## 7. Out of scope

- TypeScript implementation (does not run schema validation).
- Changing how `JsonSchemas.kt` joins/reports the error list (the `.` boundary convention is applied inside the
  messages themselves).
- Schema *load* failures (malformed schemas) — separate concern.
