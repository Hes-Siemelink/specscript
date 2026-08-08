# Command: Add

`Add` adds an item to something else

| Input  | Supported    |
|--------|--------------|
| Value  | no           |
| List   | auto-iterate |
| Object | yes          |

[Add.schema.yaml](schema/Add.schema.yaml)

## Basic usage

Add some numbers

```yaml specscript
Code example: 1 + 1 = 2

Add: [ 1, 1 ]

Expected output: 2
```

We are using the inline list syntax which is more intuitive for adding numbers. You can also use the regular list
syntax.

```yaml specscript
Code example: 1 + 1 = 2 (block list syntax)

Add:
  - 1
  - 1

Expected output: 2
```

## Add on objects

Use **Add** to extend an object with more fields.

```yaml specscript
Code example: Add a field to an object

${object}:
  1: one
  2: two

Add:
  - ${object}
  - 3: three

Expected output:
  1: one
  2: two
  3: three
```

## Adding to a list

You can add an item to a list

```yaml specscript
Code example: Add an item to a list

${list}:
  - 1
  - 2

Add:
  - ${list}
  - 3

Expected output:
  - 1
  - 2
  - 3
```

Or combine two lists.

```yaml specscript
Code example: Append a list to another

${list1}:
  - 1
  - 2
${list2}:
  - 3
  - 4

Add:
  - ${list1}
  - ${list2}

Expected output:
  - 1
  - 2
  - 3
  - 4
```

## Add to text

You can add strings to each other.

```yaml specscript
Code example: Append text

Add:
  - "Hello"
  - " World"

Expected output: Hello World
```
