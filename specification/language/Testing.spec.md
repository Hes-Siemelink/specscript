# Testing

You can run all the tests in a directory using the `spec --test` command.

```shell cli
spec --test .
```

It will report successful and failed tests.

    ```output
    All tests ran successfully.
    ```

## Test structure

A test file uses `Tests` to define named tests with nested commands. `Tests`, `Before all tests`, and `After all tests` are
only executed in test mode (`spec --test`). In normal execution they are ignored.

```yaml
Tests:
  Items are returned:
    Get: http://localhost:2525/items
    Expected output: [ 1, 2, 3 ]
```

## Setup and teardown

To set up data before any tests are run, use `Before all tests`. This is run once per file before the first test. To clean
data after all tests have run, use `After all tests`. This is run once per file after the last test. Both take nested
commands.

```yaml specscript
Code example: Tests with setup and teardown

Before all tests:
  ${sample_data}:
    items: [ 1, 2, 3 ]
    greeting: Hello

Tests:
  Check items:
    Assert that:
      item: ${sample_data.items}
      equals: [ 1, 2, 3 ]

  Check greeting:
    Assert that:
      item: ${sample_data.greeting}
      equals: Hello

After all tests:
  Print: done
```

The setup shares the same context as the tests, so variables and state carry forward. Any commands before the first test
are silently ignored in test mode.
