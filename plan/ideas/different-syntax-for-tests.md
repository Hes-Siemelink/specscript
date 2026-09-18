# Change test syntax to be more readable

## Motivation

I want to change the syntax for defining tests.

Before:

```yaml specscript
Code example: Multiple tests

Tests:
  Test 1:
    Output: one
    Expected output: one

  Test 2:
    Output: two
    Expected output: two
```

After:

```yaml specscript
Code example: Multiple tests

Tests:
  - Test case: Test 1
    Output: one
    Expected output: one

  - Test case: Test 2
    Output: two
    Expected output: two
```

This will make it a bit easier on the eyes.

See ../draft-specs/Tests.spec.md

## Context

See:

* /specscript/AGENTS.md
* /specscript/.agents/skills/specscript-coding
* /specscript-development-process
* /specscript/.agents/skills/specscript-new-command
* /specscript/.agents/skills/specscript-specs

## Implementation notes

The Tests command is a no-op because Tests run in a different context. See
/specscript/src/main/kotlin/specscript/test/TestUtil.kt for example and `cli --test` option. This mechanism is not
ideal, but leave it for now. Later we will take a look at how to properly define tests and how to run them.
