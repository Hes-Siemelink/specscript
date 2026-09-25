# Command: Tests

`Tests` defines named tests with nested commands. It takes a list of test cases. Each test case is identified by **Test
case** descriptor followed by a test script.

`Tests`, like `Before all tests` and `After all tests`, is only executed in test mode (`spec --test`). In normal
execution it is ignored.

| Input     | Supported |
|-----------|-----------|
| Value     | no        |
| List      | yes       |
| Object    | no        |
| Raw input | no        |

[Tests.schema.yaml](schema/Tests.schema.yaml)

## Basic usage

```yaml specscript
Code example: Single test case

Tests:
  - Test case: A simple test
    Assert that:
      item: one
      in: [ one, two, three ]
```

## Multiple tests

Multiple tests are defined as an array inside the `Tests` list.

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

## With setup and teardown

`Tests` works with `Before all tests` and `After all tests` to define a complete test file.

```yaml specscript
Code example: Tests with setup and teardown

Before all tests:
  ${sample_data}:
    items: [ 1, 2, 3 ]
    greeting: Hello

Tests:
  - Test case: Check items
    Assert that:
      item: ${sample_data.items}
      equals: [ 1, 2, 3 ]

  - Test case: Check greeting
    Assert that:
      item: ${sample_data.greeting}
      equals: Hello

After all tests:
  Print: done
```
