# Test CLI improvements plan

Improve the `spec --test` CLI command with proper setup/teardown support, better directory handling, and educational
samples.

## Issues identified

1. **`--test` help text is misleading** -- Says "Only tests will be executed" but `splitTestCases()` silently discards
   everything before the first `Test case:` and runs the rest. Misleading for users who put setup code before tests.

2. **Pre-test setup is silently discarded** -- `splitTestCases()` throws away commands before the first `Test case:`.
   Currently no specification test files are affected (all start with `Test case:` on line 1), but the cloud-connector
   sample has 43 lines of setup that get skipped in `--test` mode. This is a trap.

3. **Inter-test setup works by accident** -- Commands between `Test case:` blocks (like `Http request defaults` in HTTP
   client tests) get attached to the preceding test case. They work because `FileContext` is shared, but semantically
   they belong to neither test case. Fragile.

4. **Non-test files in test directories** -- Helper scripts like `greet.spec.yaml` live in `tests/` directories but
   produce empty test lists silently. Should be explicitly skipped.

5. **Test report is bare** -- Just YAML with pass/fail count. No test names printed as they run.

6. **No educational test samples** -- No `samples/test` directory exists.

## Design decisions

### New `Tests` command (replaces `Test case` going forward)

- `Test case` was a marker-style command (value content, no-op handler). Commands after it until the next `Test case`
  formed the test body. This was a clever hack but has limitations: setup before first `Test case` is silently
  discarded, and the command cannot be skipped in normal mode without breaking the commands that follow it.

- **`Tests`** holds all tests as named keys in a single object:
  ```yaml
  Tests:
    My first test:
      Assert that:
        item: one
        equals: one
    My second test:
      Output: two
      Expected output: two
  ```

- `Tests`, `Before tests`, and `After tests` are all `ObjectHandler` + `DelayedResolver` (like `Do`).
- All three are **no-ops in normal execution** -- only executed in test mode (`spec --test`).
- `Test case` remains for backward compatibility. The old `splitTestCases()` code path stays. Cleanup later.

### Test mode only

The test commands (`Tests`, `Before tests`, `After tests`) are skipped in normal execution. This means:
- Running `spec script.yaml` ignores them entirely
- Running `spec --test script.yaml` extracts and executes them
- Spec `.spec.md` code examples (which run in normal mode) can include these commands safely

## Phase 1: Spec-first -- design test commands

Write specification documents before implementing.

### Commands

- **`Tests`** -- Object content type. Each key = test name, value = nested commands.
- **`Before tests`** -- Object content type with nested commands (like `Do`). Commands run once before the first test.
  Uses `ObjectHandler` + `DelayedResolver` pattern. Runs in shared `FileContext`, state carries forward to tests.
- **`After tests`** -- Same pattern. Commands run once after the last test.

### Files created

- `specification/commands/core/testing/Tests.spec.md`
- `specification/commands/core/testing/Before tests.spec.md`
- `specification/commands/core/testing/After tests.spec.md`
- `specification/commands/core/testing/schema/Tests.schema.yaml`
- `specification/commands/core/testing/schema/Before tests.schema.yaml`
- `specification/commands/core/testing/schema/After tests.schema.yaml`
- `specification/language/Testing.spec.md` (updated)
- `specification/commands/core/testing/Test case.spec.md` (updated -- notes legacy status)

## Phase 2: Implement commands

- Create `Tests.kt` -- `ObjectHandler` + `DelayedResolver`, no-op in normal mode
- Create `BeforeTests.kt` and `AfterTests.kt` -- `ObjectHandler` + `DelayedResolver`, no-op in normal mode
- Register all three in `CommandLibrary.kt`
- Add new `splitTests()` function in `Script.kt` that handles `Tests`, `Before tests`, `After tests`
- Update `getTestCases()` in `TestUtil.kt` to use new split function when `Tests` commands are present
- Keep old `splitTestCases()` for backward compatibility with `Test case`

## Phase 3: Skip non-test files in `--test` directory mode

- In `Path.getTests()`, when processing a `.spec.yaml` file, check if it contains any `Tests` or `Test case` command.
  If not, return empty list. Already happens implicitly via splitting returning empty, but make it explicit.

## Phase 4: Create `samples/test` directory

Educational samples:

- `samples/test/basic-tests.spec.yaml` -- simple tests with assertions
- `samples/test/setup-teardown.spec.yaml` -- demonstrates `Before tests` / `After tests`
- `samples/test/.directory-info.yaml` -- directory metadata

Note: Adding files to `samples/` is safe as long as `samples/basic/` is not modified.

## Phase 5: Documentation updates

- Update `--test` description in `specscript-command-line-options.yaml`
- Cross-reference between `Tests.spec.md`, `Before tests.spec.md`, `After tests.spec.md`

## Phase 6 (optional, separate): Live test progress output

- Interactive mode (`-i`): print test names with pass/fail as they run
- Non-interactive: keep YAML output
- Separate from the core changes

## Key files

| File | Role |
|------|------|
| `src/main/kotlin/specscript/test/TestUtil.kt` | Test extraction, discovery, execution |
| `src/main/kotlin/specscript/language/Script.kt` | `splitTestCases()` / `splitTests()` -- test splitting logic |
| `src/main/kotlin/specscript/commands/testing/TestCase.kt` | `Test case` command handler (legacy) |
| `src/main/kotlin/specscript/commands/testing/Tests.kt` | `Tests` command handler (new) |
| `src/main/kotlin/specscript/commands/testing/BeforeTests.kt` | `Before tests` command handler |
| `src/main/kotlin/specscript/commands/testing/AfterTests.kt` | `After tests` command handler |
| `src/main/kotlin/specscript/cli/SpecScriptCli.kt` | CLI dispatch, `--test` handling |
| `specification/language/Testing.spec.md` | Top-level test documentation |
| `specification/commands/core/testing/Tests.spec.md` | Tests specification (new) |
| `specification/commands/core/testing/Test case.spec.md` | Test case specification (legacy) |
| `specification/cli/specscript-command-line-options.yaml` | CLI option definitions |
