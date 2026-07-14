# Command: Prompt

`Prompt` asks the user for input. It accepts a JSON Schema — the same structure used by
[Input schema](../../specification/commands/core/script-info/Input%20schema.spec.md). The
difference is where the result goes: `Prompt` returns the answer as `${output}`, while
`Input schema` populates named input variables and `${input}`.

| Input     | Supported |
|-----------|-----------|
| Value     | yes       |
| List      | no        |
| Object    | yes       |
| Raw input | yes       |

[Prompt.schema.yaml](schema/Prompt.schema.yaml)

The question shown to the user is the property's `title`. If there is no `title`, the
`description` is used, and if neither is present, the property name. When both are given, `title`
is the question and `description` is supplementary help text. Standard JSON Schema keywords
(`type`, `title`, `description`, `default`, `enum`, `format`, `required`, `properties`, `items`)
are used as-is; SpecScript-specific keywords are namespaced with an `x-` prefix
(`x-condition`, `x-env`, `x-display-property`, `x-value-property`).

## Basic usage

With **Prompt**, you can ask the user a question.

<!-- answers
What is your name?: Hes
-->

```yaml specscript
Code example: Simple question

Prompt: What is your name?

Print: Hello ${output}!
```

This will ask for user input on the command line:

```output
? What is your name? Hes
Hello Hes!
```

The string form is shorthand for a single string property. `type: string` is the default type,
so it can be omitted:

```yaml specscript
Code example: Simple question, expanded

Prompt:
  title: What is your name?

Print: Hello ${output}!
```

## Using a default value

<!-- answers
What is your name?: World
-->

```yaml specscript
Code example: Prompt with default value

Prompt:
  title: What is your name?
  default: World
```

The default value is a suggestion that is printed but can be overwritten by the user.

```output
? What is your name? World
```

In non-interactive mode a string prompt with no value returns a placeholder rather than failing,
so scripts that must have a value should supply a `default`. A choice prompt (see below) with no
value errors instead, since there is no safe default for a selection.

## Adding help text

Use `description` to add help text alongside the `title` question.

<!-- answers
Email address: info@example.com
-->

```yaml specscript
Code example: Prompt with title and help text

Prompt:
  title: Email address
  description: Enter your primary contact email
  default: info@example.com
```

## Asking for a password

Set `format: password` to mask the input as the user types.

<!-- answers
What is your password?: ssh
-->

```yaml specscript
Code example: Asking for a password

Prompt:
  title: What is your password?
  format: password
```

```output
? What is your password? ********
```

## Choosing from a list

Use `enum` to ask for one item of a list:

<!-- answers
What is your favorite color?: Red
-->

```yaml specscript
Code example: Choose one item from a list

Prompt:
  title: What is your favorite color?
  enum:
    - Red
    - Green
    - Blue
```

The user can use the cursor keys to select an item, confirming with enter.

```output
? What is your favorite color? 
 ❯ ◉ Red
   ◯ Green
   ◯ Blue
```

## Choosing multiple items

To allow multiple selections, use `type: array` with the choices under `items.enum`:

<!-- answers
What are your favorite colors?:
- Red
- Green
-->

```yaml specscript
Code example: Choose multiple items from a list

Prompt:
  title: What are your favorite colors?
  type: array
  items:
    enum:
      - Red
      - Green
      - Blue
```

Select items with the spacebar, confirm with enter. The output is an array.

```output
? What are your favorite colors? 
 ❯ ◉ Red
   ◉ Green
   ◯ Blue
```

## Choosing an object

You can pass entire objects as choices into `enum`. Specify the field used to display each choice
with `x-display-property`. The entire object is returned.

<!-- answers
Select a user: Alice
-->

```yaml specscript
Code example: Choose an object

${users}:
  - name: Alice
    id: 123
  - name: Bob
    id: 456

Prompt:
  title: Select a user
  enum: ${users}
  x-display-property: name

Print:
  You chose: ${output}
```

```output
? Select a user 
 ❯ ◉ Alice
   ◯ Bob

You chose:
  name: Alice
  id: 123
```

## Choosing only a field from an object

Use `x-value-property` to return a single field instead of the whole object.

<!-- answers
Select a user: Alice
-->

```yaml specscript
Code example: Only use the value of a specific field when selecting from an enum list

${users}:
  - name: Alice
    id: 123
  - name: Bob
    id: 456

Prompt:
  title: Select a user
  enum: ${users}
  x-display-property: name
  x-value-property: id

Print:
  You chose: ${output}
```

```output
? Select a user 
 ❯ ◉ Alice
   ◯ Bob

You chose: 123
```

## Asking multiple questions

To ask several questions at once, use an object schema with `properties`. Each property is asked
in turn and the answers are collected into the output object.

<!-- answers
First name: Juan
Last name: Pérez
-->

```yaml specscript
Code example: Multiple questions

Prompt:
  type: object
  properties:
    firstName:
      title: First name
    lastName:
      title: Last name

Print: Hello ${output.firstName} ${output.lastName}!
```

This asks two questions and produces an object, `{ "firstName": "Juan", "lastName": "Pérez" }`,
which you can capture with [As](../../specification/commands/core/control-flow/As.spec.md).

```output
? First name Juan
? Last name Pérez
Hello Juan Pérez!
```

Each property supports the full set of property keywords
(`default`, `format`, `enum`, `type: array`, `x-display-property`, `x-value-property`,
`x-condition`, `x-env`).

<!-- answers
Email address: info@example.com
Choose a color: Red
-->

```yaml specscript
Code example: Multiple questions with property options

Prompt:
  type: object
  properties:
    email:
      title: Email address
      description: Enter your primary contact email
      default: info@example.com
    color:
      title: Choose a color
      enum:
        - Red
        - Green
        - Blue
```

## Conditions

Prompts support inline conditions with `x-condition`. If the condition is false, the question is
skipped.

For a single property, a false condition skips the prompt entirely and leaves `${output}` as it
was:

```yaml specscript
Code example: Prompt with condition

Output: Already there

Prompt:
  title: What is the result?
  x-condition:
    empty: ${output}

Expected output: Already there
```

In an object schema, a skipped property is omitted from the output object. Earlier answers are
available as variables to later conditions.

```yaml specscript
Code example: Conditional and dependent questions

Answers:
  Choose which variable to set, a or b: a
  Value for a: Abracadabra
  Value for b: Borobudur

Prompt:
  type: object
  properties:
    switch:
      title: Choose which variable to set, a or b
    a:
      title: Value for a
      x-condition:
        item: ${switch}   # Refers to the answer to the first question
        equals: a
    b:
      title: Value for b
      x-condition:
        item: ${switch}
        equals: b

Expected output:
  switch: a
  a: Abracadabra

# Note: b is not set
```

## Environment variables

Use `x-env` to take the value from an environment variable, falling back to `default` when it is
not set.

```yaml specscript
Code example: Prompt with environment variable fallback

Prompt:
  title: Greeting message
  x-env: SPECSCRIPT_TEST_GREETING_NOT_SET
  default: Hello

Assert equals:
  actual: ${output}
  expected: Hello
```
