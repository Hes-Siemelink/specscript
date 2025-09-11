# SpecScript Yaml Scripts

## What does a single SpecScript file look like

### Hello world example

This is the simplest SpecScript file:

```yaml file=hello-world.cli
Print: Hello world
```

SpecScript files are a list of **commands**. In this case the file contains a single `Print` command. By convention,
commands start with a capital letter.

If we store it in a file `hello-world.cli`, we can run it with the following command:

```shell cli
cli hello-world.cli
```

And get the expected output:

```output
Hello world
```

### It's all YAML

SpecScript scripts are just Yaml.

The main idea is to blur the line between 'code' and 'data'. By having the data closer to code it makes it easier to
adopt a declarative style: saying _what_ you want to happen rather than _how_ you want it to be carried out. In this
style, you would have a higher 'data content' than 'logic content' in your scripts. Ideally, more than 50% of the script
is data, and the logic is a smaller part. Then Yaml starts making sense as a format.

By convention, commands start with a capital letter, for example `Print` or `Read file`, to distinguish them from data.

Another advantage of SpecScript syntax being Yaml is familiarity: if you have worked with Yaml before you know its
structure (and pitfalls) already, and you get editor support out-of-the-box. It's also way easier for the implementation
to parse it than a custom format.

> **⚠️ YAML Pitfall**: Since SpecScript uses YAML, you cannot repeat command names because they are dictionary keys. Use list syntax or `---` separators for multiple commands. See [Multiple commands and `---`](#multiple-commands-and---) for details.

SpecScript files have `.cli` file extension. Within SpecScript, the convention is that `.yaml` files are for static data and
`.cli` files are SpecScript files that contain commands. Tip: Map your editor to recognize `.cli`
as `.yaml`.

SpecScript's Yaml flavor does not rely on special Yaml features like directives. It is used as a human-friendly way of
writing JSON.

SpecScript does add some additional formatting on top of Yaml, for example the variable syntax `${..}`.

### The command sequence

You can have multiple commands in a file, and they will be executed in order:

```yaml file=prompt.cli
Prompt: What is your name?

Print: Hello ${output}!
```

When storing this in a file `prompt.cli` and running it with the following command,

<!-- answers
What is your name?: Hes
-->

```shell cli
cli prompt.cli
```

you would get the following:

```output
? What is your name? Hes
Hello Hes!
```

### Multiple commands and `---`

The commands in a Yaml file are just dictionary keys. We run into trouble if we want to use the same key again.

For example, this won't work because it's invalid Yaml:

```yaml
Print: Hello
Print: Hello again!  # Invalid YAML!
```

The solution is to add the Yaml document separator `---`:

```yaml specscript
Print: Hello
---
Print: Hello again!
```

It's not very pretty, but it works. This is the best option in most cases.

There are some ways to avoid the `---` separator, though.

With most commands you can simply supply the arguments as a list, and they will be executed in sequence:

```yaml specscript
Print:
  - Hello
  - Hello again!
```

<!-- Print is a bad example!!! It is a list processor designed to print lists "as lists" -->

Another approach is using the `Do` command, that takes a list of command

```yaml specscript
Do:
  - Print: Hello
  - Print: Hello again!
```

You could argue that a SpecScript file should have a giant `Do` on top, and all commands should be specified in the
list. That's perfectly fine code, but to me it looks distracting, so I prefer the separator approach. Besides, the
separator helps you to visually divide sections in your code if placed strategically.

### Script info

You can specify the description of your script with the
**[Script info](../commands/core/script-info/Script%20info.spec.md)** command.

Take this file `simple-greeting.cli`:

```yaml file=simple-greeting.cli
Script info: Prints a simple greeting

Print: Hi there!
```

When running it with the `--help` flag, the description is printed:

```shell cli
cli --help simple-greeting.cli
```

```output
Prints a simple greeting
```

### Defining script input

**Input parameters** is used to specify the script's input. Here's `greet.cli`:

```yaml file=greeting.cli
Script info: Prints a personalized greeting

Input parameters:
  name: Your name

Print: Hello ${name}!
```

It has input parameter `name`:

```shell cli
cli --help greeting.cli
```

```output
Prints a personalized greeting

Options:
  --name   Your name
```

And you can invoke it as follows.

```shell cli
cli greeting.cli --name Bob
```

```output
Hello Bob!
```

For more information, refer to the **[Script info](../commands/core/script-info/Script%20info.spec.md)** command.

### Script output

You can set the output of the script with the **[Output](../commands/core/variables/Output.spec.md)** command.

For example, with script `hello.cli`

```yaml file=hello.cli
Output:
  a: one
  b: two
  c: three
```

The output is stored in the `${output}` variable. When invoking SpecScript with the
**[--output](../cli/Command%20line%20options.spec.md#--output)** option, it will be printed:

```shell cli
cli --output hello.cli
```

```output
a: one
b: two
c: three
```

You also get it when calling a script from another script:

```yaml specscript
Run script: hello.cli

Expected output:
  a: one
  b: two
  c: three
```

