# Level 1 Implementation Report

Findings from the TypeScript Level 1 implementation (101/105 tests passing, 4 expected skips).

## Test Results

| Suite | Passed | Skipped | Failed |
|---|---|---|---|
| Level 0 | 37 | 1 | 0 |
| Level 1 | 64 | 3 | 0 |
| **Total** | **101** | **4** | **0** |

Skipped tests:
- `SCRIPT_HOME is different from SCRIPT_TEMP_DIR` — needs Temp file (Level 3)
- `Schema validation - Add should only accept arrays` — needs Validate schema (Level 5)
- `For each with variable syntax in sample data` (2 variants) — needs Read file (Level 3)

## 1. Object key resolution divergence (fixed)

**Found during testing.** The TypeScript `resolveVariables()` was resolving object keys in addition to values:

```typescript
// WRONG: resolved keys
const resolvedKey = resolveStringValue(key, variables)
result[resolvedKey] = resolveVariables(value, variables)
```

Kotlin's `JsonProcessor.processObject()` only processes values, not keys. This broke `Add to`:

```yaml
Add to:
  ${index}: 1    # key "${index}" was resolved to "0", then AddTo couldn't find the ${..} syntax
```

**Fix:** Removed key resolution from `resolveVariables()`. Keys are left as-is.

**For the Go implementer:** Only resolve values in the variable resolution pass, never keys. Object keys that
look like `${var}` are literal key names — they're interpreted by the command itself (e.g., Add to, variable
assignment).

**For the SpecScript maintainer:** This is the kind of thing that should be in the specification. The resolve
pipeline is one of the most critical and most under-documented parts of the language.

## 2. Error type coercion

Kotlin uses `toString()` on the error type, so `type: 400` (a YAML integer) becomes the string `"400"`.
The TS implementation initially only accepted string types, falling back to `"error"` for non-strings.
This broke `On error type` pattern matching — the error had type `"error"` but the handler key was `"400"`.

**Fix:** `String(data['type'])` instead of a string-only check.

**For the Go implementer:** Coerce the error type to string via `fmt.Sprint()` or equivalent. YAML integers
like `400` must become string `"400"` to match `On error type` keys (which are always strings in YAML objects).

## 3. Eval implementation notes

Eval walks the JSON tree during `resolve()`, before variable substitution. When it finds a key starting with
`/`, it strips the prefix, looks up the command handler, and executes it. The result replaces the entire object.

Key subtleties:
- Eval processes object keys before `/` — so `/Add: [1, 2]` in `{name: "Alice", /Add: [1, 2]}` first processes
  the `name` key's value recursively, then hits `/Add` and replaces the whole object with `3`.
- DelayedResolver commands skip the entire resolve pass (including eval). Their handlers must explicitly call
  `resolve()` on the portions they want evaluated.
- Nested eval works: `/Add: [/Size: [1, 2, 3], 10]` → `/Add: [3, 10]` → `13`.

The implementation was straightforward. No divergences found.

## 4. Add.add() polymorphism

The `add()` function is the most type-polymorphic function in SpecScript. It handles:

| Left | Right | Result |
|---|---|---|
| array | array | concat |
| array | value | append |
| object | object | merge (right wins) |
| string | string | concat |
| number | number | addition |
| null/undefined | any | right value |

Kotlin's implementation is in `Add.kt` (~30 lines). The TS version is ~35 lines. No edge case issues found.

`AddTo` and `Append` both delegate to `add()`. `Append` uses `context.output` as the left operand.

## 5. ForEach loop variable parsing

ForEach expects its first entry to be `${var} in: [iterable]`. The parsing logic:
1. Take the first key of the object
2. Match against regex `^\$\{(\w+)\}\s+in$`
3. If no match, treat `context.output` as the iterable and all keys as the loop body

The Kotlin implementation does NOT clean up the loop variable after the loop. The TS implementation matches
this behavior. The last iteration's value persists in `context.variables`.

**For the SpecScript maintainer:** This is likely a bug. Consider cleaning up loop variables after the loop
completes. If tests depend on leakage, those tests are testing an implementation detail.

## 6. Repeat has no termination guard

Kotlin's `Repeat` loops until its `until` condition is true, with no maximum iteration count. The TS
implementation matches this. Tests must use timeouts to avoid hangs on incorrect conditions.

The Vitest configuration uses a 10-second per-test timeout, which is sufficient for the spec test suite.

## 7. On error / On error type execution model

The error handling pipeline:
1. When a `SpecScriptCommandError` is thrown, the script loop catches it and sets `context.error`
2. Non-error-handler commands are skipped while `context.error` is set
3. When an error handler runs, it receives the raw data (unresolved, as a DelayedResolver)
4. `On error`: sets `${error}` variable, clears `context.error`, executes handler body, removes `${error}`
5. `On error type`: pattern-matches `context.error.type` against object keys, delegates matched branch to
   `runErrorHandling()`

The `On error type` command checks keys against the error type. The `any` key is a catch-all. First match wins
(iteration order of YAML object keys — which is insertion order).

## 8. Wait implementation

Kotlin uses `Thread.sleep()`. Node.js doesn't have a synchronous sleep. The TS implementation uses
`Atomics.wait()` on a SharedArrayBuffer for synchronous blocking:

```typescript
Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms)
```

This works in Node.js but would NOT work in browsers. Acceptable for a CLI tool.

## 9. Level 0 code changes for Level 1

Two changes to Level 0 code:
1. **`resolve()` exported** from `command-execution.ts` — DelayedResolver commands need to selectively resolve
   sub-expressions.
2. **`resolveVariables()` key resolution removed** from `variables.ts` — object keys are no longer resolved
   (see §1 above). This is technically a behavioral change to Level 0, but no Level 0 test depended on key
   resolution.

## 10. Files added/modified

**New files (Level 1):**
- `src/language/eval.ts` — Eval expression system (~40 lines)
- `src/commands/util.ts` — Json, Text, PrintJson, ParseYaml, Base64Encode, Base64Decode, Wait (~120 lines)
- `src/commands/data-manipulation.ts` — Add, AddTo, Append, Fields, Values, Size, Sort, Find, Replace (~290 lines)
- `src/util/json-patch.ts` — RFC 6902 implementation (~220 lines)
- `src/commands/json-patch.ts` — Json patch command wrapper (~25 lines)

**Modified files:**
- `src/language/command-execution.ts` — eval integration, resolve() exported
- `src/language/variables.ts` — key resolution removed
- `src/commands/control-flow.ts` — added If, When, ForEach, Repeat
- `src/commands/error.ts` — added OnError, OnErrorType, type coercion fix
- `src/commands/register.ts` — `registerAllCommands()` replaces `registerLevel0Commands()`
- `test/spec-runner.test.ts` — Level 1 test files, skip sets, timeout

## Summary of recommendations

### For the Go implementer (priority order)

1. Only resolve values in variable resolution, never object keys (§1)
2. Coerce error type to string (§2)
3. Get `handlesLists` right from the handler type table in the plan doc
4. Export `resolve()` so DelayedResolver commands can call it
5. Match ForEach loop variable leakage behavior (§5)
6. Use a timeout for Repeat tests (§6)

### For the SpecScript maintainer (priority order)

1. Document the resolve pipeline — what gets resolved when, key vs value behavior (§1)
2. Clean up ForEach loop variable after loop completes (§5)
3. Add a maximum iteration count to Repeat (§6)
4. Document error type coercion behavior (§2)
