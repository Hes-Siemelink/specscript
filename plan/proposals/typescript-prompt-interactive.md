# TypeScript: Prompt, Confirm, and Interactive Mode

## Problem

The TypeScript SpecScript implementation is missing the user interaction commands (`Prompt`, `Prompt object`,
`Confirm`) and the `--interactive` CLI behavior. This blocks 6 spec files from the test runner.

## Design Considerations

### TUI rendering differs between implementations

Kotlin uses `kotlin-inquirer` for fancy terminal UI (cursor-key navigation, radio/checkbox widgets). The TypeScript
implementation will use `@inquirer/prompts` for real interactive prompts.

The spec.md files contain `output` blocks that assert on simulated TUI rendering — this rendering is produced by
`TestPrompt`, not by the real TUI library. The key architectural insight:

1. **TestPrompt** — used during test/spec execution; reads from Answers map, prints deterministic simulated output
2. **RealPrompt** — used during actual CLI usage; delegates to `@inquirer/prompts` for real terminal interaction

### What TestPrompt prints

From the Kotlin `TestPrompt` source, the simulated output format is:

- **Text prompt:** `? message answer` (or `? message ********` for secrets)
- **Single select:** `? message \n ❯ ◉ selected\n   ◯ other\n   ◯ other`
- **Multi select:** `? message \n ❯ ◉ selected\n   ◉ selected\n   ◯ other`

The spec.md `output` blocks match this TestPrompt format. The TypeScript TestPrompt must produce identical output.

### RealPrompt: @inquirer/prompts

For actual interactive use, we use `@inquirer/prompts`:

- **Text prompt:** `input()` — text input with optional default
- **Password:** `password()` — masked input
- **Single select:** `select()` — cursor-key navigation list
- **Multi select:** `checkbox()` — multi-select with spacebar toggle
- **Confirm (if useful):** `confirm()` — though our Confirm command uses enum `[Yes, No]` semantics

The visual rendering of the real prompt is completely decoupled from the test assertions. The spec doesn't
prescribe the interactive UX — only the test output format (which comes from TestPrompt).

### --interactive CLI behavior

In Kotlin, `--interactive` controls two things:

1. **Directory command selection:** When `spec directory` is run without a subcommand and `--interactive` is set,
   shows an interactive menu to select a command. Currently TypeScript just prints the directory listing and exits.
2. **Input parameters fallback:** When a script has `Input parameters` and a value is missing, `--interactive`
   allows prompting for it. TypeScript already has the flag wired through and throws `MissingInputError` when
   not interactive — it just needs the prompting fallback.

### Context-based dispatch, not global mutable singleton

The Kotlin implementation uses a mutable global `UserPrompt.default` that is swapped to `TestPrompt` during test
execution, plus `Answers.recordedAnswers` as a global mutable map. This has thread-safety issues and tight
coupling between `TestPrompt` and the `Answers` command handler.

The TypeScript implementation will avoid this pattern:
- Answers are already stored in `context.session` (per-context, not global)
- The Prompt command checks the answers map first; if an answer exists, it produces TestPrompt-style output.
  If not, it delegates to `@inquirer/prompts` for real interaction.
- No global mutable state, no need to swap implementations between test and production runs.

## Implementation Plan

### 1. Add `@inquirer/prompts` dependency

```bash
pnpm add @inquirer/prompts
```

This provides `input()`, `password()`, `select()`, `checkbox()`, `confirm()` — all the interactive primitives
we need.

### 2. UserPrompt abstraction (`typescript/src/language/user-prompt.ts`)

```typescript
interface UserPrompt {
  prompt(message: string, defaultValue?: string, password?: boolean): Promise<string>
  select(message: string, choices: Choice[], multiple?: boolean): Promise<JsonValue>
}

interface Choice {
  displayName: string
  value: JsonValue
}
```

**TestPrompt** — reads from `context.session.get('answers')` map. Produces simulated TUI output matching
Kotlin's `TestPrompt` format exactly. Used when a recorded answer exists for the question.

**InquirerPrompt** — delegates to `@inquirer/prompts`. Used when no recorded answer exists (real interactive mode).

**Dispatch logic** in the prompt helper functions — not a separate "which implementation" decision. Each prompt
call checks answers first, falls through to inquirer if needed. This is simpler than maintaining two separate
UserPrompt instances.

### 3. Prompt command (`typescript/src/commands/prompt.ts`)

Handles two data forms:

- **String:** `Prompt: question` — simple text prompt
- **Object:** `Prompt: {description, default, secret, enum, select, display property, value property, condition, type}`

Resolution order (matching Kotlin's `ParameterDataPrompt`):
1. If `enum` + `select: single` (default) → single-choice selection
2. If `enum` + `select: multiple` → multi-choice selection
3. If `secret: true` → password prompt
4. If `type` has `properties` → recursive property prompting (calls prompt for each property)
5. Default → text prompt

Console output generation (for ExpectedConsoleOutput assertions):
- Write `? message answer` to stdout capture
- For select: write the full choice rendering with ❯ ◉ ◯ characters

### 4. Prompt object command (`typescript/src/commands/prompt-object.ts`)

- `delayedResolver: true` — variables in properties are resolved manually per-field
- Iterates object entries in order
- For each field: resolve variables (using accumulated answers), parse as ParameterData, check condition, prompt
- Earlier answers are available as variables for later conditions
- Returns object with all answers

### 5. Confirm command (`typescript/src/commands/confirm.ts`)

- Value-only: `Confirm: question`
- Creates enum `[Yes, No]` and delegates to single-select prompt
- Returns `"Yes"` on confirmation
- Throws `SpecScriptCommandError` with message `"No confirmation -- action canceled."` on rejection

### 6. Input parameters interactive fallback

In `script-info.ts`, where it currently throws `MissingInputError` when `!context.interactive`, add prompting
fallback using the same prompt infrastructure.

### 7. CLI --interactive directory menu

In `cli.ts` `invokeDirectory`, when `subcommands.length === 0`:
- If `options.interactive && !options.help`: use `@inquirer/prompts` `select()` to show interactive command menu
- Otherwise: print directory listing (current behavior)

### 8. Test runner considerations

**No changes needed to the test runner itself** — the existing `Expected console output` command already
captures stdout and compares. The Prompt commands write to stdout via the existing capture mechanism. Since
answers are pre-seeded via `Answers` commands in the spec files, the TestPrompt path is always taken during
spec test execution.

## Files to Create/Modify

| File | Action | Size |
|------|--------|------|
| `typescript/src/language/user-prompt.ts` | Create — prompt abstraction, TestPrompt logic, InquirerPrompt logic | M |
| `typescript/src/commands/prompt.ts` | Create — Prompt command | M |
| `typescript/src/commands/prompt-object.ts` | Create — Prompt object command | S |
| `typescript/src/commands/confirm.ts` | Create — Confirm command | S |
| `typescript/src/commands/register.ts` | Modify — register 3 new commands | XS |
| `typescript/src/commands/script-info.ts` | Modify — interactive fallback for Input parameters | S |
| `typescript/src/cli.ts` | Modify — interactive directory menu | S |
| `typescript/test/spec-runner.test.ts` | Modify — add 6 spec files, remove from HIGHER_LEVEL_COMMANDS | S |
| `typescript/package.json` | Modify — add `@inquirer/prompts` dependency | XS |

## Spec Files Unlocked

1. `commands/core/user-interaction/Prompt.spec.md` (7 sections)
2. `commands/core/user-interaction/Prompt object.spec.md` (5 sections)
3. `commands/core/user-interaction/Confirm.spec.md` (2 sections)
4. `commands/core/user-interaction/tests/Prompt tests.spec.yaml` (7 tests)
5. `commands/core/user-interaction/tests/Prompt object tests.spec.yaml` (4 tests)
6. `commands/core/testing/Answers.spec.md` (1 section — uses Prompt in its example)

## Risk Areas

- **TestPrompt output format:** Must match Kotlin's `TestPrompt` output exactly (the `?` prefix, spacing,
  ❯ ◉ ◯ characters). One mismatch breaks spec.md output assertions.
- **Prompt with `type: {properties: ...}`** — recursive prompting. The "Prompt with custom type" test in
  Prompt tests.spec.yaml exercises this. Need to handle nested property definitions.
- **Prompt object DelayedResolver** — must manually resolve variables per field with accumulated context.
  This is the same pattern as Kotlin's `PromptObject.execute()`.
- **Boolean handling in choices** — Prompt object tests.spec.yaml has a "Boolean choice" test with
  `enum: [true, false]`. The answer is `"true"` (string) but expected output is `true` (boolean).
  Need to match the original choice value, not the string answer.

## Kotlin Cleanup Opportunity (separate ticket, main branch)

The Kotlin prompt architecture has some cleanup opportunities that align with the cleaner patterns being
introduced in TypeScript. This would be a separate bean on the `main` branch.

### Current Kotlin issues

1. **Global mutable state:** `UserPrompt.default` is a mutable `var` swapped to `TestPrompt` during test
   execution. `Answers.recordedAnswers` is a global mutable map that accumulates across test cases without
   clearing. Neither is thread-safe.

2. **Tight coupling:** `TestPrompt` directly accesses `Answers.recordedAnswers` (the raw map field).
   `InputParameters` also directly accesses `Answers.hasRecordedAnswer()`/`getRecordedAnswer()`, creating
   a second bypass path. Two separate subsystems coupled through shared global state.

3. **Single-file multi-concern:** `UserPrompt.kt` contains the interface, the companion service-locator,
   `KInquirerPrompt`, `TestPrompt`, and `renderInput` helper functions — mixing abstraction, two
   implementations, and test-output formatting in one file.

4. **Dead code:** `InputParameters.handleInputType()` is empty. Commented-out lines in `UserInteraction.kt`.

### Suggested Kotlin cleanup

- Thread `UserPrompt` through `ScriptContext` instead of global `var` (matching TypeScript's context-based
  approach and Kotlin's own `UserInput` constructor injection pattern)
- Store answers in context instead of `Answers.recordedAnswers` global map (TypeScript already does this)
- Extract `TestPrompt` and `KInquirerPrompt` into separate files
- Move `renderInput` helpers to `TestPrompt`'s file
- Delete dead code
