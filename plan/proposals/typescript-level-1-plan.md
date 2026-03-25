# TypeScript Implementation Plan — Level 1: Control Flow and Data

## Scope

Level 1 adds 22 commands + Eval syntax on top of Level 0's core runtime. After Level 1, SpecScript-TS is a complete
data-processing scripting language — branching, iteration, error handling, and data transformation.

**Exit criteria:**

- All 22 commands + Eval syntax implemented
- `pnpm test` runs Level 0 + Level 1 spec tests and they pass
- Test count: ~825 lines across 14 test files (plus Level 0's existing 37 tests)

---

## What Level 1 Adds

### Eval Syntax (language feature)

Inline command execution during the `resolve()` phase. Keys starting with `/` are treated as command invocations,
executed in-place, and replaced with their result.

```yaml
Output:
  /Add: [1, 2, 3]   # resolves to 6 before Output sees it
```

This integrates into `command-execution.ts`'s `resolve()` function, running *before* variable substitution.
DelayedResolver commands skip eval (their bodies are not resolved until explicitly executed by the handler).

### Commands by Category

| Category | Commands | Count |
|---|---|---|
| Control flow | If, When, For each, Repeat | 4 |
| Error handling | On error, On error type | 2 |
| Data manipulation | Add, Add to, Append, Fields, Find, Json patch, Replace, Size, Sort, Values | 10 |
| Utility | Json, Text, Print Json, Parse Yaml, Base64 encode, Base64 decode, Wait | 7 |

**Total: 23 commands + 1 language feature**

---

## Handler Type Mapping

Every command must declare the correct `handlesLists` value. This table is the authoritative reference for Level 1.

| Command | handlesLists | delayedResolver | errorHandler | Notes |
|---|---|---|---|---|
| If | false | **yes** | — | Extracts then/else, resolves condition only |
| When | **true** | **yes** | — | Array of conditions, first match wins |
| For each | false | **yes** | — | Loop variable via `${var} in` syntax |
| Repeat | false | **yes** | — | Loops until condition met |
| On error | false | **yes** | **yes** | Sets `${error}`, clears context.error |
| On error type | false | **yes** | **yes** | Pattern-matches error type via keys |
| Add | **true** | — | — | Polymorphic: array concat, object merge, string concat, number addition |
| Add to | false | — | — | Mutates named variables. Calls `Add.add()` |
| Append | **true** | — | — | Adds to context.output. Calls `Add.add()` |
| Fields | false | — | — | Returns array of keys. Falls back to context.output |
| Find | false | — | — | JSON Pointer path navigation |
| Json patch | false | — | — | RFC 6902. Falls back to context.output |
| Replace | false | — | — | Recursive text find-and-replace. Falls back to context.output |
| Size | **true** | — | — | Returns length/count/identity-for-numbers |
| Sort | false | — | — | Sorts array of objects by single field. Falls back to context.output |
| Values | false | — | — | Returns array of values from object |
| Json | **true** | — | — | Converts to compact JSON string |
| Text | **true** | — | — | Converts to YAML display string |
| Print Json | **true** | — | — | Pretty-prints JSON to stdout. Returns null |
| Parse Yaml | false | — | — | Parses string as YAML/JSON |
| Base64 encode | false | — | — | Standard Base64 |
| Base64 decode | false | — | — | Standard Base64 |
| Wait | false | — | — | Sleeps N seconds. Returns null |

7 commands handle lists (When, Add, Append, Size, Json, Text, Print Json).
6 commands are DelayedResolvers (all control flow + error handlers).
2 commands are ErrorHandlers (On error, On error type).

---

## Build Order

### Phase 1: Eval Syntax

Eval must come first — it modifies the `resolve()` pipeline that all subsequent commands use.

**Changes to existing code:**

1. Add `eval()` function in `src/language/eval.ts` — walks a JSON tree, finds keys starting with `/`, strips prefix,
   executes the command, replaces the node with the result.
2. Modify `resolve()` in `command-execution.ts` to call `eval()` before variable resolution.
3. Eval must NOT recurse into DelayedResolver command bodies.

**Implementation:**

```typescript
// src/language/eval.ts
export function evalExpressions(data: JsonValue, context: ScriptContext): JsonValue {
  if (!isObject(data)) {
    if (isArray(data)) return data.map(item => evalExpressions(item, context))
    return data
  }

  for (const [key, value] of Object.entries(data)) {
    const evaluatedValue = evalExpressions(value, context)
    data[key] = evaluatedValue

    if (key.startsWith('/')) {
      const commandName = key.substring(1)
      const handler = context.getCommandHandler(commandName)
      const result = runCommand(handler, evaluatedValue, context)
      return result ?? ''
    }
  }

  return data
}
```

**Resolve pipeline change:**

```typescript
// command-execution.ts - resolve()
function resolve(data: JsonValue, context: ScriptContext): JsonValue {
  const copied = deepCopy(data)
  const evaluated = evalExpressions(copied, context)
  return resolveVariables(evaluated, context.variables)
}
```

**Tests:** `specification/language/tests/Eval tests.spec.yaml` (75 lines)

### Phase 2: Utility Commands (7 commands)

These are the simplest commands — no dependencies on other Level 1 commands. Good warm-up and immediately useful.

**Build order within phase:**

1. **Json** — `JSON.stringify(data)`. 3 lines of logic.
2. **Text** — Convert to display YAML. Reuses `toDisplayYaml` from Level 0.
3. **Print Json** — `JSON.stringify(data, null, 2)` → stdout. Returns null.
4. **Parse Yaml** — `yaml.parse(stringValue)`. Graceful fallback on parse error.
5. **Base64 encode** — `Buffer.from(str).toString('base64')`
6. **Base64 decode** — `Buffer.from(str, 'base64').toString('utf-8')`
7. **Wait** — `await new Promise(resolve => setTimeout(resolve, seconds * 1000))` or synchronous sleep.

Note on Wait: The Kotlin implementation is synchronous (`Thread.sleep()`). In Node, we either need
`Atomics.wait()` for sync sleep or convert the execution model to async. Since Level 0 is synchronous,
use `Atomics.wait()` for now.

**Tests:** `specification/commands/core/util/tests/Base64 tests.spec.yaml` (13 lines),
`specification/commands/core/util/tests/Wait tests.spec.yaml` (5 lines).
Json/Text/PrintJson/ParseYaml have no dedicated test files — tested via inline spec.md examples (Level 2).

**Files:**

- `src/commands/util.ts` — all 7 commands in one file

### Phase 3: Data Manipulation (10 commands)

This is the largest group. `Add.add()` is the foundational function — AddTo and Append both call it.

**Build order (dependency-driven):**

1. **Add** — the core. Polymorphic addition: array concat, object merge, string concat, number addition. Expose
   `add()` as a named export.
2. **Add to** — calls `Add.add()`, mutates named variables in context. Must use `${var}` syntax for the target.
3. **Append** — calls `Add.add()` with context.output as the base. Returns the combined result.
4. **Fields** — returns `Object.keys(data)` or `Object.keys(context.output)`. Falls back to context.output when input
   is a value (string path, not used in practice).
5. **Values** — returns `Object.values(data)`. Complement of Fields.
6. **Size** — returns `.length` for arrays/strings, key count for objects, identity for numbers.
7. **Sort** — sorts array of objects by a single field (ascending only). Falls back to context.output.
8. **Find** — JSON Pointer path navigation. Depends on `toJsonPointer()` from Kotlin's Variables utility.
9. **Replace** — recursive text find-and-replace. Falls back to context.output.
10. **Json patch** — full RFC 6902 implementation. This is the most complex command (~250 lines). Falls back to
    context.output for the document.

**Tests:** 6 test files, ~275 total lines. Key ones: Add tests (150 lines, 14 test cases), Replace tests (38 lines),
Size tests (34 lines).

**Files:**

- `src/commands/data-manipulation.ts` — Add, AddTo, Append, Fields, Values, Size, Sort, Find, Replace
- `src/util/json-patch.ts` — RFC 6902 implementation (standalone utility)
- `src/commands/json-patch.ts` — Json patch command wrapper

### Phase 4: Control Flow (4 commands)

All four are DelayedResolvers — they receive unresolved data and selectively resolve parts of it.

**Build order:**

1. **If** — the core. Extracts `then`/`else` keys, resolves remaining keys as conditions, executes matching branch.
   Expose `evaluate()` as a named export for When to reuse.
2. **When** — array of conditions. Iterates, runs `If.evaluate()` on each, stops at first match. `else` key acts as
   default.
3. **For each** — loop variable syntax: first key is `${var} in`, value is the iterable. Remaining keys are the loop
   body. Returns mapped list/object.
4. **Repeat** — loops until `until` condition is met. Always returns null. No iteration limit.

**Critical implementation detail:** DelayedResolver commands must manually call `resolve()` on the portions they want
to evaluate. In the TS implementation, this means importing and calling the `resolve()` function (or equivalent) from
`command-execution.ts`. This function must be exported.

**For each loop variable:** The loop variable (`${item}` in `${item} in`) is set in `context.variables` during
iteration. Kotlin does NOT clean it up after the loop (it leaks). Follow the same behavior.

**Tests:** If tests (179 lines, includes When tests), For each tests (148 lines), Repeat tests (39 lines), Schema
tests (51 lines).

**Files:**

- `src/commands/control-flow.ts` — extend existing file with If, When, ForEach, Repeat

### Phase 5: Error Handling (2 commands)

Both are DelayedResolvers AND ErrorHandlers — they execute even when `context.error` is set.

**Build order:**

1. **On error** — sets `${error}` variable from `context.error`, clears `context.error`, executes the handler body,
   removes `${error}`. Expose `runErrorHandling()` for OnErrorType to reuse.
2. **On error type** — pattern-matches error type via object keys. `any` is catch-all. Delegates matched branch to
   `OnError.runErrorHandling()`.

**Critical implementation detail:** The existing script execution loop in `script.ts` must already handle
`errorHandler` — it should check `context.error` and skip non-error-handler commands when an error is active.
Verify this is already implemented in Level 0 (it should be, since `ExpectedError` is an error handler).

**Tests:** Error handling tests (42 lines).

**Files:**

- `src/commands/error.ts` — extend existing file with OnError, OnErrorType

### Phase 6: Test Runner Updates

1. Add all 14 Level 1 test files to the test runner's file list.
2. Update `registerLevel0Commands()` → `registerAllCommands()` or add a `registerLevel1Commands()` function.
3. Run `pnpm test` and fix any failures.

**Level 1 test files to add:**

```
language/tests/Eval tests.spec.yaml
commands/core/control-flow/tests/If tests.spec.yaml
commands/core/control-flow/tests/For each tests.spec.yaml
commands/core/control-flow/tests/Repeat tests.spec.yaml
commands/core/control-flow/schema/Schema tests.spec.yaml
commands/core/errors/tests/Error handling tests.spec.yaml
commands/core/data-manipulation/tests/Add tests.spec.yaml
commands/core/data-manipulation/tests/Append tests.spec.yaml
commands/core/data-manipulation/tests/Json patch tests.spec.yaml
commands/core/data-manipulation/tests/Replace tests.spec.yaml
commands/core/data-manipulation/tests/Size tests.spec.yaml
commands/core/data-manipulation/tests/Sort tests.spec.yaml
commands/core/util/tests/Base64 tests.spec.yaml
commands/core/util/tests/Wait tests.spec.yaml
```

### Phase 7: Implementation Report

Write `plan/proposals/level-1-implementation-report.md` — same format as the Level 0 report. Document:

- Divergences found between Kotlin and TS behavior
- Spec ambiguities or gaps discovered during implementation
- Recommendations for the Go implementer (priority-ordered)
- Recommendations for the SpecScript maintainer (priority-ordered)
- Any changes made to Level 0 code to support Level 1

---

## Internal Dependency Graph

```
Add.add()  ←── AddTo
     ↑
     └──── Append (also reads context.output)

If.evaluate()  ←── When

OnError.runErrorHandling()  ←── OnErrorType

Eval ─── modifies resolve() pipeline ─── affects all non-DelayedResolver commands

context.output ←── Fields, Sort, Json patch, Replace, Append (fallback reads)
```

---

## Key Implementation Notes

### resolve() must be exportable

Control flow commands need to call `resolve()` from `command-execution.ts` on sub-expressions. Currently it's a
private function. Export it, or add a `resolveData(data, context)` function to the `ScriptContext` interface.

Recommendation: Add to ScriptContext:

```typescript
interface ScriptContext {
  // ... existing ...
  /** Resolve variables and eval expressions in data */
  resolve(data: JsonValue): JsonValue
}
```

This keeps the resolve pipeline encapsulated and accessible to DelayedResolver commands.

### Script execution loop and error handling

The script execution loop (`script.ts`) must skip non-error-handler commands when `context.error` is set.
Verify Level 0 already does this. If not, add it now — it's a prerequisite for On error/On error type.

### context.output fallback pattern

Several data manipulation commands (Fields, Sort, Json patch, Replace, Append) read `context.output` as a fallback
when no explicit input is provided (or when certain keys reference the "current" value). This is a common Kotlin
pattern using `context.variables[OUTPUT_VARIABLE]`. The TS equivalent is `context.output`.

### Synchronous execution model

Level 0 is fully synchronous. Level 1 stays synchronous. The only command that *wants* async is Wait, and we solve
that with `Atomics.wait()` (or just skip the actual sleep in test mode and verify the test expectations match).

---

## Timeline Estimate

| Phase | Duration | Cumulative | What's working |
|---|---|---|---|
| 1: Eval syntax | 0.5 day | 0.5 day | `/Command` inline expressions |
| 2: Utility commands | 0.5 day | 1 day | Json, Text, PrintJson, ParseYaml, Base64, Wait |
| 3: Data manipulation | 1.5 days | 2.5 days | Add, AddTo, Append, Fields, Find, JsonPatch, Replace, Size, Sort, Values |
| 4: Control flow | 1 day | 3.5 days | If, When, ForEach, Repeat |
| 5: Error handling | 0.5 day | 4 days | OnError, OnErrorType |
| 6: Test runner + fixes | 0.5 day | 4.5 days | All Level 1 tests green |
| 7: Implementation report | 0.5 day | 5 days | Findings documented |

5 working days total. Json patch is the wildcard — RFC 6902 is ~250 lines in Kotlin and could take longer.

---

## Implementation Report: Level 0 Learnings Applied to Level 1

### Carried forward from Level 0

These findings from the Level 0 implementation report directly affect Level 1 work.

**1. Auto-list iteration remains the biggest trap.**
Level 1 adds 7 more `handlesLists: true` commands (When, Add, Append, Size, Json, Text, Print Json). The handler type
table in this document is the authoritative reference — don't infer from test results. Level 0 found that Print, Exit,
Error, and ExpectedError were wrong but tests passed by accident. The same risk exists for Level 1 commands.

**2. Auto-list null handling is still unspecified.**
Kotlin drops nulls from auto-iterated results; TS preserves them. Level 1 adds more commands that return null
(Wait, Add to, Repeat) which increases exposure. The TS implementation should continue preserving nulls (matching
Level 0 behavior) but this divergence needs a spec decision eventually.

**3. Missing property path behavior affects control flow.**
`${obj.missing}` returns `""` in TS. This matters for If/When conditions — a missing property evaluates as empty
string (falsy in `is` comparisons but truthy in `is not empty`). Kotlin's `MissingNode` has different truthiness.
Watch for condition evaluation divergences in If/When tests.

**4. resolve() export is a new requirement.**
Level 0 didn't need to export `resolve()` — no command called it. Level 1's DelayedResolver commands (If, When,
For each, Repeat, On error, On error type) all need to selectively resolve sub-expressions. This is the first
structural change to the Level 0 runtime.

### Level 1-specific risks

**5. Eval interaction with DelayedResolver.**
Eval runs during `resolve()`, which is skipped for DelayedResolver commands. This means `/Command` expressions inside
If/When/ForEach bodies are NOT evaluated at parse time — they're evaluated when the branch/loop body is explicitly
resolved by the handler. This is correct but subtle. The risk: if a DelayedResolver handler calls `resolve()` on a
sub-expression, eval runs on THAT sub-expression. If it forgets to call resolve(), eval is silently skipped.

**6. Add.add() polymorphism.**
`Add.add()` is the most polymorphic function in SpecScript. It handles: array + array (concat), object + object
(merge), string + string (concat), number + number (addition), and mixed-type combinations. The Kotlin implementation
is ~50 lines. The TS implementation needs to match all type combinations exactly, including edge cases like
`null + array` and `string + number`.

**7. Json patch is the complexity outlier.**
RFC 6902 (`JsonPatcher.kt`) is 252 lines in Kotlin — more than any other single command. It supports add, remove,
replace, move, copy, and test operations with JSON Pointer path navigation. This is a self-contained utility that
should be implemented and unit-tested independently before wiring up the command.

**8. For each loop variable leaks.**
Kotlin's ForEach sets the loop variable in `context.variables` and does NOT remove it after the loop. This means
`${item}` is still accessible after the loop (with the value from the last iteration). This is likely a bug, not
intentional — but tests may depend on it. Match the Kotlin behavior.

**9. Repeat has no iteration guard.**
Kotlin's Repeat loops until `until` is true with no maximum iteration count. An incorrect `until` condition creates
an infinite loop. Not our problem to fix at implementation time, but worth noting for test timeouts.

**10. Sort is ascending-only, single-field.**
Kotlin's Sort command sorts by a single field in ascending order. No descending, no multi-field sort. This is a
deliberate simplification. Don't over-engineer it.

### Recommendations for Level 1 implementation

1. **Implement Eval first** — it changes the resolve pipeline and affects all non-delayed commands.
2. **Export resolve()** from command-execution.ts as a public function before starting Phase 4.
3. **Unit-test Add.add() polymorphism** exhaustively — it's the foundation for 3 commands.
4. **Implement Json patch as a standalone utility** with its own unit tests before wiring the command.
5. **Verify error handler skip logic** in script.ts before implementing On error — it should already exist from
   Level 0's ExpectedError, but confirm it works for the On error use case (clearing error, running body, etc.).

---

## What NOT to Build at Level 1

- **Markdown parsing** — Level 2
- **File I/O** — Level 3
- **HTTP** — Level 4
- **CLI features beyond `--test`** — not until Level 3
- **npm publishing** — not yet
