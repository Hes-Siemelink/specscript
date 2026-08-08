# Proposal: Built-in commands accept a plain value via a default property (TS port + full spec)

## Status

Follow-up to [x-default-property.md](./x-default-property.md). The script-level shorthand was implemented and spec'd
(`💫 Scripts accept a plain value via a default property`, commit `2033832`). The command-level mechanism was
implemented in Kotlin only (`💫 Built-in commands accept a plain value via a default property (implementation only --
needs spec)`, commit `15b77dc`). This proposal covers the missing work: the **TypeScript port** and the **full spec
documentation**.

## 1. What the Kotlin commit did

- Added `defaultProperty: String? = null` to `CommandHandler`
  (`src/main/kotlin/specscript/language/CommandHandler.kt:14`).
- Dispatch: in `handleCommand`, an `ObjectHandler` receiving a `ValueNode` with a `defaultProperty` wraps the scalar into
  `{ <defaultProperty>: <scalar> }` before execution (`src/main/kotlin/specscript/language/CommandExecution.kt:93`).
- Converted `ReadFile` from `ValueHandler + ObjectHandler` → `ObjectHandler` with `defaultProperty = "file"`
  (`src/main/kotlin/specscript/commands/files/ReadFile.kt:14`).
- Annotated `specification/commands/core/files/schema/Read file.schema.yaml` with `x-default-property: file`.

Behavior is user-identical to the old `ValueHandler` path — a plain value to `Read file` is now routed through the
`file` property instead of a dedicated value handler.

## 2. Current state

### Spec documentation

Already done in the previous commit (`2033832`):

- `GET.spec.md` → `Default property: url` table row; `HttpParameters.schema.yaml` has `x-default-property: url`.
- `Run.spec.md` → `Default property: script` table row; `Run.schema.yaml` has `x-default-property: script`.
- `Input schema.spec.md` documents the mechanism for scripts (the object stays an object in both forms).

Not done:

- `Read file.spec.md` — no `Default property` row; its support table says `Object: no` which contradicts its own
  object-form examples (bug).
- All other commands that accept a plain value have no `x-default-property` annotation and no table row.

### TypeScript implementation

- Behaviorally already correct: `ReadFileCommand` (`typescript/src/commands/files.ts:106`) accepts plain values via
  `resolvePath`'s string branch, and the full TS spec-runner passes (532 passed, 6 skipped).
- **Architecturally missing:** the `defaultProperty` concept does not exist in the TS `CommandHandler` interface
  (`typescript/src/language/command-handler.ts`) and there is no wrapping in `handleCommand`
  (`typescript/src/language/command-execution.ts`).
- All command dispatch funnels through one spot (`command-execution.ts:77`), so one wrapping point covers everything.

## 3. Proposed solution

### 3.1 TypeScript port

1. `typescript/src/language/command-handler.ts` — add `defaultProperty?: string` to the `CommandHandler` interface.
2. `typescript/src/language/command-execution.ts` — in `handleCommand`, when `handler.defaultProperty` is set and
   `isScalar(data)` (exists in `typescript/src/language/types.ts:31`), wrap into `{ [defaultProperty]: data }` before
   `handler.execute`. Mirrors the Kotlin `ValueNode` check; arrays are already auto-iterated upstream
   (`runCommand`), so they never reach `handleCommand` for non-list commands.
3. `typescript/src/commands/files.ts` — set `defaultProperty: 'file'` on `ReadFileCommand`.
4. Add `typescript/test/files.test.ts`:
   - Read file with a plain value (temp YAML file → parsed content).
   - Object forms `{ file }` / `{ resource }` still work.
   - Stub-handler test verifying scalar → `{ defaultProperty: value }` wrapping.

### 3.2 Full spec documentation

Follow the GET/Run pattern: `x-default-property` annotation in the command schema plus a `Default property` table row.
Scope (value form verified in both implementations):

| Command            | Default property | Notes                                   |
|--------------------|------------------|-----------------------------------------|
| `Read file`        | `file`           | Also fix `Object: no` → `yes` in table. Schema already annotated. |
| `Write file`       | `file`           | value form = filename                   |
| `Temp file`        | `content`        |                                         |
| `Cli`              | `command`        |                                         |
| `Shell`            | `command`        |                                         |
| `Error`            | `message`        |                                         |
| `Expected error`   | `message`        |                                         |
| `Prompt`           | `description`    | see 3.3 — shared schema                 |

Excluded: `POST` / `DELETE` (support table says `Value: no`). `GET` and `Run` already done.

### 3.3 Prompt — shared schema caveat

`Prompt.schema.yaml` is a bare `$ref` to the shared `ParameterData.schema.yaml` (also used by `Confirm`). Adding the
annotation there would also declare it for `Confirm` (consistent — its value form is also the question text, i.e.
`description`). Two options:

- Add `x-default-property: description` as a `$ref` sibling in `Prompt.schema.yaml` only (draft 2020-12 allows `$ref`
  siblings). Matches the proposal's scope exactly.
- Add it to the shared `ParameterData.schema.yaml`, covering both `Prompt` and `Confirm`.

Recommendation: `$ref` sibling on `Prompt.schema.yaml` only.

## 4. No Kotlin runtime work needed for the other commands

The remaining commands already hand-implement `ValueHandler` and satisfy the documented behavior. The `x-default-property`
annotation is documentary — command schema validation is currently disabled
(`// FIXME Schema validation does not work`, `src/main/kotlin/specscript/language/CommandExecution.kt:60`). Only
`ReadFile` uses the runtime `defaultProperty` mechanism.

## 5. Verification

- Kotlin: `./gradlew specificationTest`
- TypeScript: `npm test` in `typescript/`

## Out of scope

- `plan/TODO.md`: "Pull default property from command schema. (Currently defined on the code level)" — future
  refactor to read `defaultProperty` from the schema instead of the constructor.
- Converting the remaining Kotlin commands from `ValueHandler` to the `defaultProperty` mechanism (behavior-neutral;
  `ReadFile`-only is the current scope).
- The proposal's `type: [object, string]` schema restyle (existing schemas use `oneOf`; keep minimal).
