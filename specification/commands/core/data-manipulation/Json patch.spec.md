# Command: Json patch

Applies [RFC 6902 JSON Patch](https://datatracker.ietf.org/doc/html/rfc6902) operations to a JSON document. Supports
`add`, `remove`, `replace`, `move`, `copy`, and `test` operations.

| Input      | Supported     |
|------------|---------------|
| Scalar     | no            |
| List       | no            |
| Object     | yes           |

[Json patch.schema.yaml](schema/Json%20patch.schema.yaml)

## Basic usage

Apply a patch to a document provided via the `doc` parameter.

```yaml specscript
Code example: Add a field to an object

Json patch:
  doc:
    name: Alice
  patch:
    - op: add
      path: /age
      value: 30

Expected output:
  name: Alice
  age: 30
```

## Patching the output variable

When `doc` is omitted, the command patches the current output variable.

```yaml specscript
Code example: Patch the output variable

Output:
  city: Berlin

Json patch:
  patch:
    - op: add
      path: /country
      value: Germany

Expected output:
  city: Berlin
  country: Germany
```

## Remove operation

Remove a field from a document.

```yaml specscript
Code example: Remove a field

Json patch:
  doc:
    name: Alice
    age: 30
    temp: discard
  patch:
    - op: remove
      path: /temp

Expected output:
  name: Alice
  age: 30
```

## Replace operation

Replace an existing value.

```yaml specscript
Code example: Replace a value

Json patch:
  doc:
    status: draft
  patch:
    - op: replace
      path: /status
      value: published

Expected output:
  status: published
```

## Move operation

Move a value from one location to another.

```yaml specscript
Code example: Move a field

Json patch:
  doc:
    old_name: Alice
  patch:
    - op: move
      from: /old_name
      path: /name

Expected output:
  name: Alice
```

## Copy operation

Copy a value to a new location.

```yaml specscript
Code example: Copy a field

Json patch:
  doc:
    name: Alice
  patch:
    - op: copy
      from: /name
      path: /display_name

Expected output:
  name: Alice
  display_name: Alice
```

## Multiple operations

Apply several operations in sequence.

```yaml specscript
Code example: Multiple patch operations

Json patch:
  doc:
    items:
      - apple
      - banana
    count: 2
  patch:
    - op: add
      path: /items/-
      value: cherry
    - op: replace
      path: /count
      value: 3

Expected output:
  items:
    - apple
    - banana
    - cherry
  count: 3
```

## Patching arrays

JSON Patch works with arrays using numeric indices and the `-` token to append.

```yaml specscript
Code example: Patch an array

Json patch:
  doc:
    - first
    - second
  patch:
    - op: add
      path: /1
      value: inserted

Expected output:
  - first
  - inserted
  - second
```
