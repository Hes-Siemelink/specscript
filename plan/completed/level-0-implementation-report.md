# Level 0 Implementation Report

Findings from the TypeScript Level 0 implementation (37/38 tests passing) with recommendations
for the Go implementer and the SpecScript maintainer.

## 1. Auto-list iteration: the hidden dispatch rule

When a command receives an array and doesn't opt into list handling, the runtime auto-iterates:
it calls the handler once per element and collects results. This is controlled by
`AnyHandler`/`ArrayHandler` in Kotlin, `handlesLists: true` in TS.

**This is the single most surprising behavior in Level 0.** It's implicit, undocumented at the
command level, and produces bugs that are hard to trace. Example: variable assignment
`${list}: [1, 2, 3]` — without `handlesLists: true`, the runtime iterates and the variable
ends up as `3`, not `[1, 2, 3]`.

### For the Go implementer

Every command must explicitly decide: does it handle arrays itself, or does the runtime iterate?
There is no safe default. Here is the correct mapping for Level 0 commands:

| Command | Handles lists? | Why |
|---|---|---|
| Output | yes | Passes arrays through as-is |
| Assignment (`${var}:`) | yes | Stores the value, including arrays |
| Do | yes | Treats the array as a flat script |
| Exit | yes | Throws Break with the full value |
| Print | yes | Prints the whole value as YAML |
| Expected output | yes | Compares against the full output |
| Expected error | yes (rejects) | Should throw "arrays not allowed" |
| Error | yes (rejects) | Should throw "arrays not allowed" |
| All others | no | Auto-iterate is correct |

The TS implementation had bugs here: Print, Exit, Error, and ExpectedError were all wrong.
Tests happened to pass because no Level 0 test exercises these with array input. Don't rely
on the test suite to catch this — get it right from the start.

### For the SpecScript maintainer

**Recommendation:** Add a `content-types` field to each command's spec, listing which types
it accepts: `value`, `object`, `array`, `any`. This would serve as the source of truth for
implementers and could be verified programmatically.

Currently, you have to read the Kotlin source to know whether a command implements
`ArrayHandler` vs `AnyHandler` vs `ObjectHandler`. The spec documents mention "Content type"
tables but they don't cover the auto-list behavior.

### Auto-list null handling divergence

When auto-iterating, Kotlin drops null results from the output array; the TS implementation
fills with `null`. Given `SomeCommand: [a, b, c]` where `b` returns null:
- Kotlin: `[resultA, resultC]`
- TS: `[resultA, null, resultC]`

**Recommendation:** Spec this. The Kotlin behavior (dropping nulls) is surprising and loses
positional alignment. The TS behavior (preserving nulls) is more predictable. Pick one and
document it.

## 2. Missing property path behavior

When navigating into a variable via dot notation and a property doesn't exist:

- Kotlin: returns empty (`MissingNode`, renders as `""`)
- TS: returns `""` (mimicking Kotlin)

This is lenient and useful for `${env.MAYBE_UNSET}`, but it means typos in variable paths
are silently swallowed: `${user.nmae}` returns `""` instead of failing.

### Chained missing paths

`${obj.missing.nested}`:
- Kotlin: `MissingNode.at("/nested")` returns another `MissingNode` → empty
- TS: tries to navigate `.nested` on the string `""` → throws

**For the Go implementer:** Decide upfront whether missing properties return a "missing" sentinel
or an empty string. If you use a sentinel (recommended), make sure it propagates through further
navigation (like Kotlin's `MissingNode`).

**For the SpecScript maintainer:** This is worth specifying explicitly. The lenient behavior is
needed for `env` but debatable for user variables. Consider: missing `env` properties → empty;
missing user variable properties → error.

## 3. Input parameters and Input schema

These two commands share the same population logic. The Kotlin implementation has `InputSchema`
delegate to `InputParameters.populateInputVariables()`. The TS implementation duplicates the
logic in a shared function.

### Answers integration

Kotlin's `InputParameters` checks `Answers.recordedAnswers` as a fallback before throwing for
missing values. The TS implementation does NOT check answers. This means test scripts like:

```yaml
Answers:
  What is your name?: Alice
Input parameters:
  name:
    description: What is your name?
```

...work in Kotlin but fail in TS with `MissingInputError`.

**For the Go implementer:** Implement the Answers lookup in InputParameters from the start. The
fallback chain is: existing input → condition check → env var → default → recorded answers →
interactive prompt → error.

**For the SpecScript maintainer:** The Answers fallback is undocumented in the Input parameters
spec. It should be, since it's essential for non-interactive testing.

### Kotlin Answers is a static singleton

`Answers.recordedAnswers` is a `mutableMapOf` on the companion object — global mutable state.
This works for single-threaded spec test execution but is a concurrency hazard. The TS
implementation scopes answers to `context.session`, which is cleaner.

**Recommendation:** Move Answers storage to the session/context in Kotlin too.

## 4. Expected error semantics differ

Kotlin's `ExpectedError` with object input uses the object keys as error type matchers:

```yaml
Expected error:
  any: true           # matches any error type
  missing-input: true # matches only missing-input errors
```

The TS implementation treats the object as field-based assertions:

```yaml
Expected error:
  message: "something went wrong"
  type: missing-input
```

These are fundamentally different APIs. The Kotlin version can't assert on error messages;
the TS version can't use `any:` as a catch-all.

**For the Go implementer:** Follow the Kotlin semantics (key-based type matching) since that's
what the spec tests use. But flag this for the maintainer.

**For the SpecScript maintainer:** The `Expected error` spec should clarify the object format.
Both approaches have merit. Consider supporting both: keys as type matchers, and reserved keys
(`message:`, `type:`) as field assertions.

## 5. TestCase/CodeExample don't reset stdout in TS

Kotlin's `TestCase` and `CodeExample` call `ExpectedConsoleOutput.reset(context)` to clear
captured stdout. The TS implementation doesn't reset. This means console output from previous
test cases leaks into subsequent `Expected console output` assertions.

Not caught by Level 0 tests because the test runner splits test cases into separate Vitest
`it()` blocks with fresh contexts. But this would matter for flat test files with multiple
`Test case:` / `Expected console output:` pairs executed in a single context.

**For the Go implementer:** Reset captured output on TestCase and CodeExample boundaries.

## 6. Test structure observations

### Cross-level dependencies in Level 0 test files

`Variables tests.spec.yaml` uses `Temp file` (Level 3). This is the only `.spec.yaml` test
file in Level 0 with a cross-level dependency. All other cross-level issues are in `.spec.md`
files which are expected to have partial coverage.

**For the SpecScript maintainer:** Consider moving the "SCRIPT_HOME is different from
SCRIPT_TEMP_DIR" test to a Level 3 test file. It tests `Temp file` behavior, not variable
behavior. The `SCRIPT_HOME` test that remains is sufficient for Level 0.

### Tests: object-based vs flat format

Level 0 uses two test structures:
1. **Structured** (`Tests:` with named sub-keys) — used by most test files
2. **Flat** (`Test case:` boundaries) — used by `empty.spec.yaml`

The structured format has a problem: YAML objects can't have duplicate keys. So you can't
have two test cases that both start with `Output:`. This isn't a Level 0 issue specifically,
but it constrains how tests can be written.

### env variable initialization

The `env` built-in variable needs to be populated at context creation time, not lazily. Both
Kotlin and TS set it during context construction. For Go, do the same — populate `env` with
`os.Environ()` at context creation.

## 7. Minor items

### `resolve()` deep copies before variable substitution

Both Kotlin and TS deep-copy the command data before resolving variables. This is correct
(prevents mutation of the parsed script) but expensive. For Go, consider copy-on-write or
structural sharing if performance matters.

### Type dispatch vs unified handler

Kotlin dispatches to different `execute()` overloads based on input type (`ValueNode`,
`ObjectNode`, `ArrayNode`). TS uses a single `execute(data: JsonValue)` and the handler does
its own type checking. The TS approach is simpler. Go should follow the TS approach — a single
`Execute(data any, ctx *Context) (any, error)` method.

### YAML duplicate key preservation

Standard YAML parsers do last-wins for duplicate keys. SpecScript scripts use duplicate keys
as separate commands. Both Kotlin and TS solve this by walking the YAML AST. For Go, use
`gopkg.in/yaml.v3` and walk `yaml.Node` directly — don't use `Unmarshal` for command parsing.

## Summary of recommendations

### For the Go implementer (priority order)

1. Get `handlesLists` right for every command from the start (table in §1)
2. Walk YAML AST for command parsing, not `Unmarshal` (§7)
3. Populate `env` and `SCRIPT_HOME` at context creation (§6)
4. Implement Answers fallback in InputParameters (§3)
5. Reset stdout capture in TestCase/CodeExample (§5)
6. Use key-based type matching for ExpectedError objects (§4)
7. Decide on missing-property behavior upfront (§2)

### For the SpecScript maintainer (priority order)

1. Add content-type declarations to command specs (§1)
2. Specify auto-list null-handling behavior (§1)
3. Document Answers fallback in InputParameters spec (§3)
4. Clarify ExpectedError object format (§4)
5. Move the Temp file test out of Variables tests (§6)
6. Specify missing-property path behavior (§2)
7. Move Answers from static singleton to session context (§3)
