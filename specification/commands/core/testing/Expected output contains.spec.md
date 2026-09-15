# Command: Expected output contains

`Expected output contains` tests if the `${output}` variable contains a given value

| Input  | Supported |
|--------|-----------|
| Value  | yes       |
| List   | no        |
| Object | yes       |

[Expected output contains.schema.yaml](schema/Expected%20output%20contains.schema.yaml)

## Basic usage

**Expected output contains** is a shortcut for [Assert that](Assert%20that.spec.md#contains) `in`, comparing against
`${output}`

It works for simple values

```yaml specscript
Code example: Check if output list contains a value

Output:
  - one
  - two
  - three

Expected output contains: one
```

Also for complex objects

```yaml specscript
Code example: Check if output object contains a subset

Output:
  1: one
  2: two
  3: three

Expected output contains:
  1: one
  2: two
```

### Equivalence to Assert that

**Expected output contains** is equivalent to **[Assert that](Assert%20that.spec.md#contains)** with `item` /
`in: ${output}`

```yaml specscript
Code example: Equivalence to Assert that

Output:
  1: one
  2: two
  3: three

Assert that:
  item:
    1: one
    2: two
  in: ${output}
```
