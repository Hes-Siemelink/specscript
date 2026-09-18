# Proposal: Change the Tests syntax to a list of named test cases

## Status

Implemented idea: [plan/ideas/different-syntax-for-tests.md](../ideas/different-syntax-for-tests.md). A working draft
spec already exists at [plan/draft-specs/Tests.spec.md](../draft-specs/Tests.spec.md). This proposal covers the full
change for both implementations.

## Problem

The current `Tests` command takes a map (test name -> body):

```yaml
Tests:
  Test 1:
    Output: one
    Expected output: one
```

This is less readable than a list of explicitly named test cases:

```yaml
Tests:
  - Test case: Test 1
    Output: one
    Expected output: one

  - Test case: Test 2
    Output: two
    Expected output: two
```

## Proposed solution

Change the `Tests` command to take a **list** of test cases. Each test case is an object whose `Test case` key holds the
test name; the remaining keys are the test body commands. The old map form is **dropped** (breaking change: `Object:
no`), per the draft spec's input table.

The name reuses the existing `Test case` descriptor so the new form reads naturally and stays close to the legacy
`Test case` command.

## Scope

### Spec

- Move the draft spec to the command reference: update
  `specification/commands/core/testing/Tests.spec.md` to the list syntax.
- Update `specification/commands/core/testing/schema/Tests.schema.yaml` to describe a list of `{ text: "Test case",
  object: body }` elements.
- Update all spec files that show `Tests:` examples:
  `specification/language/Testing.spec.md`, `Before all tests.spec.md`, `After all tests.spec.md`,
  `Expected console output.spec.md`, `specification/overview/specscript-overview-agents.md`.
- Update `README.md` and `README-old.md` (their code examples run in the test suite).

### Migrate existing test files

All test files that use the old map syntax are converted to the list form. Roughly 45 `.spec.yaml` test files under
`specification/**/tests/` and 11 under `samples/**/tests/`. Helper scripts without `Tests:` (e.g.
`create-test-db.spec.yaml`, `setup-test-credentials.spec.yaml`) are unchanged.

Note: `specification/commands/core/control-flow/schema/Schema tests.spec.yaml` also uses the old syntax and is migrated.

### Implementations

- Kotlin: update `splitTests()` in `src/main/kotlin/specscript/language/Script.kt` to parse the list form (name from
  `Test case`, body from the remaining keys). `TestUtil.kt` already consumes `NamedTest`s and needs no change.
- TypeScript: update `splitTests()` in `typescript/src/language/script.ts` and the test extraction in
  `typescript/src/cli.ts` (`runYamlTests`) and `typescript/test/spec-runner.test.ts` (`runStructuredTests`).

## Out of scope

- The legacy `Test case` command stays as-is (still supported).
- The test-harness mechanism (`spec --test`, `Tests` as a no-op in normal execution) is unchanged;
  `plan/draft-specs/Tests.spec.md` implementation notes still apply.
- `plan/archive/Demos and Messaging/README-new-messaging.md` is archived and not run by the test suite; left unchanged.