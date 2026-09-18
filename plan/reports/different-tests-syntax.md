# Report: List syntax for `Tests:` (breaking change)

## What changed

`Tests:` no longer accepts a map of `name: body`. It is now a list of test cases:

```yaml
Tests:
  - Test case: Test 1
    Display: Hello
```

With `Test case` as a required descriptor holding the test name, and the remaining
keys of the element forming the test body. Old map syntax is dropped entirely — no
fallback, no deprecation.

## Migration

All existing tests in `specification/**/tests/*.spec.yaml` and `samples/**/tests/*.spec.yaml`
were migrated to the list syntax with a script, preserving names and bodies (verified by
parsing: every `Tests:` value is a list of `Test case` elements with semantics identical
to the old map).

Two files had list bodies and were converted by hand:
`samples/contacts/tests/get-tests.spec.yaml` and `samples/goals-app/tests/get-tests.spec.yaml`.

Spec documents (`Tests.spec.md`, `Testing.spec.md`, Before/After, Expected console output,
overview, both READMEs) were updated to show the new syntax.

## Test results

- Kotlin `./gradlew specificationTest` — all pass (573 tests). This executes every migrated
  sample in the spec docs AND every migrated YAML test file, so one broken migration would
  have surfaced here.
- Kotlin `./gradlew test` — all pass.
- TypeScript `npm test` — 568 pass, 6 skipped, 0 fail.

## Implementation notes

- Kotlin `splitTests()` parses each array element: key `Test case` is the test name, the
  remaining keys become the body. Missing `Test case` falls back to "unnamed".
- TypeScript `splitTests()` was rewritten to mirror Kotlin: it now returns
  `{setup, tests: NamedTest[], teardown}`. This also removed a long-standing divergence —
  the TS version kept stray commands found between the test sections, while Kotlin ignored
  them. Both now ignore them.
- The `Tests.schema.yaml` uses `additionalProperties: true` (not `{type: object}`) because
  body values are scalars (strings, lists), and the schema validator runs per element when
  a script validates each command.
- This is a breaking change: old map-syntax `.spec.yaml` files will report malformed tests.
  Quoted map keys as tests are broken; they must be rewritten as list entries.

## Things the reviewer might want to look at

- The two hand-converted list-body files (contacts, goals-app) — they are the only ones with
  this shape.
- TypeScript `runYamlTests`/`runStructuredTests` were simplified; check the change removes
  the old per-name handling cleanly.
- `npx tsc --noEmit` reports pre-existing type errors unrelated to this change
  (`cli.ts:537`, `cli.ts:779`, `mcp-server.ts`, `package-registry.ts:237`) — untouched here.

## Note on the final sweep

A naive grep for "old syntax remnants" initially flagged many files, but it was a false
alarm: it matched `key: value` command lines *inside* test bodies, which are correct in the
new syntax. The trusted check is that both test suites execute every migrated file in the
new syntax and pass.