# Proposal: add support for type:string in Input schema

## Introduction

Currently the Input schema command only supports object types. This proposal suggests adding support for string types as
well.

This would align it with the Prompt command, that support both sting and object.

```yaml specscript
Code example: Input schema with string type

Input schema:
  type: string
  title: User name
  description: Name to use for greeting
  default: Stranger

Assert equals:
  actual: ${input}
  expected: Stranger

Print: Hello ${input}!

```

## Calling scripts with String input

Updating the Input schema spec is one thing, but that's one side of the coin. We cover the use cases in the spec where a
script that takes input is being called.

Suppose we have defined the above example in a file `greet-user.cli.md`.

```yaml temp-file=greet-user.cli.md
Script info: Greets a user

Input schema:
  type: string
  title: User name
  description: Name to use for greeting
  default: Stranger

Output: Hello ${input}!
```

### Using Run command

When using Run command: (see specscript/specification/commands/core/files/Run.spec.md)

```yaml specscript
Code example: Run script with string input

Run:
  script: greet-user.cli.md
  input: Hes

Expected output: Hello Hes!
```

### Using generated command

or the 'magic command' case: (See 'Calling another script' in specification/language/Organizing SpecScript files in
directories.spec.md)

```yaml specscript
Code example: Run script as command with string input

Greet user: Hes

Expected output: Hello Hes!
```

### CLI invocation

And also CLI invocation. Here we need to find a way to pass inout as string without parameter name. we need to discuss
the alternatives and pick at least one.

One way would be to use a blessed parameter name, like `--input` or `-i`.

```cli cd=${SCRIPT_TEMP_DIR}
spec greet-user --input Hes
```

Another one to use a positional argument. (Looks nice but may be a head ache to parse reliably)

```cli cd=${SCRIPT_TEMP_DIR}
spec greet-user Hes
```

### Shell pipe

The there is the case to pipe input to the script. Maybe out of scope for now -- implementation is doable but unclear
how to test! Since you need shell to run the test but against wich version of specscript? We have the ```cli directive
to loop back into running version of specscript but that is not a shell.

```
echo Hes | spec greet-user
```

### MCP

Next question: what if you have such a script and what to expose it as a MCP? How would you pass the input string to the
script? I suppose many MCP clients will break and then better not to solve this but have best practice to use object
anyway.

### Default parameter

One tiny loophole that can grow big is to designate a parameter as the 'default string case'. This will help MCP but
also streamline definition of other commands.

```yaml specscript
Code example: Default parameter for string input

Input schema:
  type: object
  properties:
    name:
      type: string
    email:
      type: string
  x-default-parameter: name
```

How this would work is that if you call the script with a string, it will be assigned to the default parameter. If you
call it with an object, it will be assigned to the properties as usual.

This could help a more predictable way to document value parameter on commands.

For example:

* Run command: x-default-parameter would be `script`
* [Shell](/specscript/specification/commands/core/shell/Shell.spec.md): x-default-parameter would be `command`
* [GET](/specscript/specification/commands/core/http/GET.spec.md): x-default-parameter would be `url`

etc.

Sping this out as a separate proposal. Maybe we need to do this first. Note that it looks like x-default-parameter must
be a value type like string, otherwise it would get really weird because you replace one object with another.

## Notes

See [Agent handoff](../reports/input-schema-string-input-handoff.md) for more context.