# Command: Before all tests

`Before all tests` runs commands once before all tests in a file. The setup commands are nested inside the `Before all tests`
block, similar to the `Do` command.

`Before all tests`, like `Tests` and `After all tests`, is only executed in test mode (`spec --test`). In normal execution
it is ignored.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | implicit  |
| Object       | yes       |

[Before all tests.schema.yaml](schema/Before%20all%20tests.schema.yaml)

## Basic usage

Place `Before all tests` before `Tests`. The nested commands run once before the first test. The setup shares
the same context as the tests, so variables and state carry forward.

```yaml specscript
Code example: Before all tests with Http request defaults

Before all tests:
  Http request defaults:
    url: http://localhost:2525

Tests:
  Get items:
    Get: /items
    Expected output: [ 1, 2, 3 ]

  Get hello:
    Get: /hello
    Expected output: Hello from SpecScript!
```
