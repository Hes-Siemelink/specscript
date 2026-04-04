# Command: After all tests

`After all tests` runs commands once after all tests in a file. The teardown commands are nested inside the
`After all tests` block, similar to the `Do` command.

`After all tests`, like `Tests` and `Before all tests`, is only executed in test mode (`spec --test`). In normal
execution it is ignored.

| Input     | Supported    |
|-----------|--------------|
| Value     | no           |
| List      | auto-iterate |
| Object    | yes          |
| Raw input | yes          |

[After all tests.schema.yaml](schema/After%20all%20tests.schema.yaml)

## Basic usage

Place `After all tests` after `Tests`. The nested commands run once after the last test completes. The teardown shares
the same context as the tests.

```yaml specscript
Code example: After all tests for cleanup

Before all tests:
  ${data}:
    started: true

Tests:
  Verify setup ran:
    Assert that:
      item: ${data.started}
      equals: true

After all tests:
  Print: Cleanup complete
```

## Combined with Before all tests

`Before all tests` and `After all tests` work together to bracket your tests with shared initialization and cleanup.
