# Command: Tests

`Tests` defines named tests with nested commands. Each key is a test name and its value contains the commands
to execute.

`Tests`, like `Before all tests` and `After all tests`, is only executed in test mode (`spec --test`). In normal execution
it is ignored.

| Input      | Supported     |
|------------|---------------|
| Scalar     | no            |
| List       | no            |
| Object     | yes           |
| Raw input  | yes           |

[Tests.schema.yaml](schema/Tests.schema.yaml)

## Basic usage

```yaml specscript
Code example: Simple tests

Tests:
  A simple test:
    Assert that:
      item: one
      in: [ one, two, three ]
```

## Multiple tests

Multiple tests are defined as sibling keys inside `Tests`.

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

## With setup and teardown

`Tests` works with `Before all tests` and `After all tests` to define a complete test file.

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
