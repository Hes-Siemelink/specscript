# Running SpecScript files

You run a SpecScript file with the `spec` command. See [Running SpecScript](/README.md#running-specscript) for setup
instructions.

## Global options

When running `spec` or `spec --help`, the global options will be printed

```cli
spec
```

```output
SpecScript -- Create instantly runnable specs using Yaml and Markdown!

Usage:
   spec [global options] file | directory [command options]

Global options:
  --help, -h           Print help on a script or directory and does not run anything
  --no-output, -n      Suppress the output at the end of the script
  --output-json, -j    Print the output at the end of the script in Json format
  --interactive, -i    SpecScript may prompt for user input if it needs more information
  --debug, -d          Run in debug mode. Prints stacktraces when an error occurs.
  --test, -t           Run in test mode. Only tests will be executed.
  --package-path, -p   Directory containing packages
```

For more information on the options, see [Command line options](Command%20line%20options.spec.md)

### Running a single file

In the **[specification](..)** directory, there is a file
**[hello-world.spec.yaml](../hello-world.spec.yaml)** that contains a simple "Hello World" command:

```yaml
Print: Hello World!
```

Run it with the following command

```cli cd=..
spec hello-world.spec.yaml
```

And you will see this output:

```output
Hello World!
```

You can omit the `.spec.yaml` extension to make it look more like a "cli command":

```cli cd=..
spec hello-world
```

```output
Hello World!
```

## Running a directory

In the samples directory, there is a subdirectory **[basic](../code-examples/basic)** with more SpecScript examples.

Running SpecScript on a directory shows the commands that are available in that directory.

```cli cd=../code-examples
spec basic
```

```output
Simple SpecScript example scripts

Available commands:
  create-greeting   Creates a greeting and puts it in the output
  greet             Prints a greeting
  multiple-choice   Interaction example
  output            Sets test output
  prompt            Simple interactive prompt
```

Once you know which script you want to execute, simply chain them as commands on the command line. For example, to
execute the `greet.spec.yaml` script in the `basic` directory, do:

```cli cd=../code-examples
spec basic greet
```

This will give the expected output:

```output
Hello, World!
```

----------------------------------------------------------------------

## Supplying input

Some scripts take input. Use the [--help](Command%20line%20options.spec.md#--help) option to list the supported
parameters

```cli cd=../code-examples
spec --help basic greet
```

```output
Prints a greeting

Options:
  --name   Your name
```

With that information we can give the script some custom input:

```cli cd=../code-examples
spec basic greet --name Alice
```

This will print:

```output
Hello, Alice!
```