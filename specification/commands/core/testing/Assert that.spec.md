# Command: Assert that

`Assert that` executes a condition. Conditions can also be used in other commands like [If](../control-flow/If.spec.md)
and [Repeat](../control-flow/Repeat.spec.md)

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | implicit  |
| Object       | yes       |

[Assert that.schema.yaml](schema/Assert%20that.schema.yaml)

## Basic usage

**Assert that** throws an exception if the condition is not true and execution of the script is stopped.

## Conditions

### Object equals

Compare two objects, one is in field `item`; the other in `equals`.

```yaml specscript
Code example: Comparing values

Assert that:
  item: one
  equals: one
```

This is also works for lists

```yaml specscript
Code example: Comparing lists

Assert that:
  item:
    - one
    - two
  equals:
    - one
    - two
```

and objects

```yaml specscript
Code example: Comparing objects

Assert that:
  item:
    one: 1
    two: 2
  equals:
    one: 1
    two: 2
```

### Contains

You can also test if something is inside something else with the `'item'` and `in`.

```yaml specscript
Code example: Check if an object is in a list

Assert that:
  item: one
  in:
    - one
    - two
    - three
```

You can also test for parts of an object.

```yaml specscript
Code example: Check if an object contains some properties

Assert that:
  item:
    one: 1
    two: 2
  in:
    one: 1
    two: 2
    three: 3
```

### Empty

Check if an array or value is empty

```yaml specscript
Code example: Empty list and string

Assert that:
  - empty: [ ]
  - empty: ""
```

It's a shorthand for checking equality. This makes more sense in an `If` statement when you are checking the value that
is coming from somewhere else.

```yaml specscript
Code example: If with empty

${values}: [ ]

If:
  empty: ${values}
  then:
    Exit: Nothing to process
```

### All, Any and Not

The conditions **all**, **any** and **not** take other conditions as a subcondition.

The **all** condition is a logical AND.

```yaml specscript
Code example: All conditions

Assert that:
  all:
    - item: one
      equals: one
    - item: two
      equals: two
```

The **any** condition is a logical OR.

```yaml specscript
Code example: Any condition

Assert that:
  any:
    - item: one
      equals: one
    - item: three
      equals: four
```

The **not** condition is the negation

```yaml specscript
Code example: Not condition

Assert that:
  not:
    empty:
      - one
      - two
```

## Multiple assertions

When you need multiple assertions, use list syntax instead of repeating the command:

```yaml specscript
Code example: Multiple assertions with list syntax

${config}:
  database:
    host: localhost
    port: 5432
  features:
    authentication: true
    logging: true

Assert that:
- not:
    empty: ${config.database.host}
- item: ${config.features.authentication}
  equals: true
- item: ${config.database.port}
  equals: 5432
```


