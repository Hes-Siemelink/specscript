# Command: Answers

`Answers` prerecords answers for prompts, so they can pass automated tests.

| Input  | Supported |
|--------|-----------|
| Value  | no        |
| List   | no        |
| Object | yes       |

[Answers.schema.yaml](schema/Answers.schema.yaml)

## Basic usage

`Answers` provides values for `Input parameters` so scripts can run non-interactively in tests.

```yaml specscript
Code example: Answers provides input parameter values

Answers:
  Your name: Alice

Input parameters:
  name:
    description: Your name

Assert equals:
  actual: ${name}
  expected: Alice
```

## Prerecording prompt answers

When testing scripts that use `Prompt`, `Answers` prevents the test from hanging on user input.

```yaml specscript
Code example: Prerecord an answer to a prompt

Answers:
  What is your name?: Alice

Prompt: What is your name?

Expected output: Alice
```
