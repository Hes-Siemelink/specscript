# Command: After tests

`After tests` runs commands once after all tests in a file. The teardown commands are nested inside the
`After tests` block, similar to the `Do` command.

`After tests`, like `Tests` and `Before tests`, is only executed in test mode (`spec --test`). In normal execution
it is ignored.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | implicit  |
| Object       | yes       |

[After tests.schema.yaml](schema/After%20tests.schema.yaml)

## Basic usage

Place `After tests` after `Tests`. The nested commands run once after the last test completes. The teardown
shares the same context as the tests.

```yaml specscript
Code example: After tests for cleanup

Before tests:
  ${data}:
    started: true

Tests:
  Verify setup ran:
    Assert that:
      item: ${data.started}
      equals: true

After tests:
  Print: Cleanup complete
```

## Combined with before tests

`Before tests` and `After tests` work together to bracket your tests with shared initialization and cleanup.
