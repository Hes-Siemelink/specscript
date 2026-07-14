# Command: Prompt

`Prompt` asks the user for input. It takes a JSON Schema — the same structure as
[Input schema](../script-info/Input%20schema.spec.md) — and returns the answer as
`${output}`. (`Input schema` uses the identical schema but stores the result in named input variables instead.)

| Input     | Supported |
|-----------|-----------|
| Value     | yes       |
| List      | no        |
| Object    | yes       |
| Raw input | yes       |

[Prompt.schema.yaml](schema/Prompt.schema.yaml)

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

This asks for input on the command line:

```output
? What is your name? Hes
Hello Hes!
```

The short form is shorthand to ask for a single string property. We use JSON Schema to define input,
like [Input schema](../script-info/Input%20schema.spec.md). The above example is
equivalent to:

```yaml specscript
Code example: Simple question, expanded

Prompt:
  type: string
  title: What is your name?
```

The `type: string` is the default type, so it can be omitted.

## Using a default value

A `default` is a suggestion. In interactive mode it pre-fills the question and the user can overwrite it; in
non-interactive mode it is used as the answer.

<!-- answers
What is your name?: World
-->

```yaml specscript
Code example: Prompt with default value

Prompt:
  title: What is your name?
  default: World
```

```output
? What is your name? World
```

## Adding help text

`title` is the question; `description` adds help text alongside it.

<!-- answers
Email address: info@example.com
-->

```yaml specscript
Code example: Prompt with help text

Prompt:
  title: Email address
  description: Your primary contact email
  default: info@example.com
```

## Asking for a password

Set `format: password` to mask the input.

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

Use `enum` to choose one item.

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

```output
? What is your favorite color? 
 ❯ ◉ Red
   ◯ Green
   ◯ Blue
```

## Choosing multiple items

Use `type: array` with the choices under `items` to allow multiple selections. The output is an array.

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

```output
? What are your favorite colors? 
 ❯ ◉ Red
   ◉ Green
   ◯ Blue
```

## Choosing from a list of objects

Pass objects into `enum` and name the field to show with `x-title-property`. The whole object is returned.

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
  x-title-property: name

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

Add `x-value-property` to return a single field instead of the whole object.

<!-- answers
Select a user: Alice
-->

```yaml specscript
Code example: Return only one field of the chosen object

${users}:
  - name: Alice
    id: 123
  - name: Bob
    id: 456

Prompt:
  title: Select a user
  enum: ${users}
  x-title-property: name
  x-value-property: id

Print: You chose ${output}
```

```output
? Select a user 
 ❯ ◉ Alice
   ◯ Bob

You chose 123
```

## Asking multiple questions

Use an object schema with `properties` to ask several questions at once. The answers are collected into an output
object.

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

```output
? First name Juan
? Last name Pérez
Hello Juan Pérez!
```

Each property takes the full set of keywords, so questions can mix defaults, choices and help text.

## Conditional questions

Use `x-condition` to skip a question when the condition is false. In an object schema, earlier answers are available as
variables, so a later question can depend on an earlier one.

```yaml specscript
Code example: A question that depends on a previous answer

Answers:
  Which value to set?: a
  Value for a: Abracadabra
  Value for b: Borobudur

Prompt:
  type: object
  properties:
    switch:
      title: Which value to set?
    a:
      title: Value for a
      x-condition:
        item: ${switch}
        equals: a
    b:
      title: Value for b
      x-condition:
        item: ${switch}
        equals: b

Expected output:
  switch: a
  a: Abracadabra
```

## Missing values

When no value can be resolved and there is no `default`, a text question falls back to a placeholder in non-interactive
mode, while a choice question raises an error (there is no safe default for a selection). Supply a `default` for
questions that must always have a value.

## JSON Schema keyword mapping

The following property keywords are supported. Standard JSON Schema keywords are used as-is; SpecScript-specific ones
carry an `x-` prefix.

| Keyword              | Purpose                                                                      |
|----------------------|------------------------------------------------------------------------------|
| `title`              | The question shown to the user                                               |
| `description`        | Help text (the question falls back to this, then the property name)          |
| `type`               | `string` (default), `array` (multiple choice), `object` (multiple questions) |
| `default`            | Suggested value: a hint in interactive mode, the fallback otherwise          |
| `format`             | `password` masks the input                                                   |
| `enum`               | List of choices                                                              |
| `items`              | Holds the `enum` of choices when `type: array`                               |
| `x-condition`        | Skip the question when the condition is false                                |
| `x-title-property` | Field to show when choosing from a list of objects                           |
| `x-value-property`   | Return only this field of the chosen object                                  |
