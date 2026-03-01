# Command: Input schema

`Input schema` defines the input parameters of a script using standard JSON Schema syntax. It is the recommended way to
define input parameters for new scripts, replacing the older `Input parameters` command.

| Content type | Supported |
|--------------|-----------|
| Value        | no        |
| List         | no        |
| Object       | yes       |

[Input.schema.yaml](schema/Input%schema.schema.yaml)

## Basic usage

Define the input for a script using a standard JSON Schema object with `type`, `properties`, and optionally `required`.

<!-- yaml specscript
${input}:
 name: world
-->

```yaml specscript
Code example: Basic Input schema usage

Input schema:
  type: object
  properties:
    name:
      description: Your name

Print: Hello, ${name}!
```

When running this, there are three possibilities:

1. The variable `${name}` is provided as input. In that case all is good and nothing happens.
2. The variable `${name}` is not provided, and the script is run in interactive mode. Then the user is prompted with the
   question **Your name** and the result is stored in the `${name}` variable.
3. The variable `${name}` is not provided, and the script is not run in interactive mode. Then an error is thrown and
   the script is aborted.

## Cli help

Use `cli --help` to see the description of the parameters.

Let's put the above example in a file called `schema-input.spec.yaml`:

```yaml file=schema-input.spec.yaml
Script info: A script with input schema

Input schema:
  type: object
  properties:
    name:
      description: Your name

Print: Hello, ${name}!
```

Then running

```shell cli
spec --help schema-input.spec.yaml
```

Should print:

```output
A script with input schema

Options:
  --name   Your name
```

## Multiple variables

You can define multiple input properties at once.

<!-- yaml specscript
${input}:
   greeting: Hello
   name: world
-->

```yaml specscript
Code example: Define input with multiple variables using schema

Input schema:
  type: object
  properties:
    greeting:
      description: What is your greeting?
    name:
      description: What is your name?

Print: ${greeting}, ${name}!
```

## The input variable

Input properties are also stored in the `${input}` variable, just like with `Input parameters`.

<!-- yaml specscript
${input}:
   greeting: Hello
   name: world
-->

```yaml specscript
Code example: Input schema with direct variable access

Input schema:
  type: object
  properties:
    greeting:
      description: What is your greeting?
    name:
      description: What is your name?

Print: ${input.greeting}, ${input.name}!
```

## Default values

Use `default` on individual properties to specify a fallback value.

```yaml specscript
Code example: Input schema with default value

Input schema:
  type: object
  properties:
    name:
      description: What is your name?
      default: World

Assert equals:
  actual: ${name}
  expected: World
```

## Required properties

Use the `required` array to mark properties that must be provided. Properties not listed in `required` that have no
`default` will still prompt in interactive mode, but won't raise an error in non-interactive mode.

<!-- yaml specscript
${input}:
  name: Alice
  greeting: Hello
-->

```yaml specscript
Code example: Input schema with required properties

Input schema:
  type: object
  properties:
    name:
      description: Your name
    greeting:
      description: Your greeting
      default: Hello
  required:
    - name

Print: ${greeting}, ${name}!
```

## Enum values

Use `enum` to restrict the allowed values. In interactive mode, this renders as a selection list.

<!-- yaml specscript
${input}: {}
-->

```yaml specscript
Code example: Input schema with enum

Input schema:
  type: object
  properties:
    priority:
      description: Priority level
      enum: [ low, medium, high ]
      default: medium

Assert equals:
  actual: ${priority}
  expected: medium
```

## Variables and conditions

You can define input depending on other input properties being set. The properties that are being referred to need to be
defined before the property that uses them. You can refer to them as part of the `${input}` variable, for example
`${input.otherVariable}`.

This example uses `${input.switch}` to determine which variable will be part of the input. By setting `switch` to `a`,
`property-A` is set but not `property-B`.

<!-- yaml specscript
${input}: { }
-->

```yaml specscript
Code example: Input schema with variables and conditions

Input schema:
  type: object
  properties:
    switch:
      description: Choose a or b
      default: a
    property-A:
      description: What is the value for A?
      default: Ananas
      condition:
        item: ${input.switch}
        equals: a
    property-B:
      description: What is the value for B?
      default: Bologna
      condition:
        item: ${input.switch}
        equals: b

Assert equals:
  actual: ${input}
  expected:
    switch: a
    property-A: Ananas
```

## Compatibility with MCP tool definitions

The `Input schema` command uses the same JSON Schema syntax as MCP tool `inputSchema` definitions. This means the schema
you write for your script's input can be reused directly when exposing the script as an MCP tool, eliminating the need
to define parameters twice.

```yaml
# In your script file:
Input schema:
  type: object
  properties:
    title:
      type: string
      description: Goal title
  required: [ title ]

# In your MCP server definition — same schema:
Mcp tool:
  create_goal:
    description: Create a new goal
    inputSchema:
      type: object
      properties:
        title:
          type: string
          description: Goal title
      required: [ title ]
    script: goals/create.spec.yaml
```
