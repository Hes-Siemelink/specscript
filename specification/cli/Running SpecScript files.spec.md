# Running SpecScript files

You run an SpecScript file with the `cli` command.

## Global options

When running `cli` or `cli --help`, the global options will be printed

```shell cli
cli
```

```output
SpecScript -- Create instantly runnable specs using Yaml and Markdown!

Usage:
   cli [global options] file | directory [command options]

Global options:
  --help, -h          Print help on a script or directory and does not run anything
  --output, -o        Print the output at the end of the script in Yaml format
  --output-json, -j   Print the output at the end of the script in Json format
  --non-interactive, -q   Indicates that SpecScript should not prompt for user input
  --debug, -d         Run in debug mode. Prints stacktraces when an error occurs.
```

For more information on the options, see [Command line options](Command%20line%20options.spec.md)

### Running a single file

In the **[samples](/samples)** directory, there is a file **[hello.cli](/samples/hello.cli)** that contains a simple "
Hello World" command:

```yaml file=hello.cli
Print: Hello from SpecScript!
```

After [installing SpecScript](/README.md#build--run), run it with the following command

```shell cli cd=samples
cli hello.cli
```

And you will see this output:

```output
Hello from SpecScript!
```

You can omit the `.cli` extension to make it look more like a "cli command":

```shell cli cd=samples
cli hello
```

```output
Hello from SpecScript!
```

## Running a directory

In the samples directory, there is a subdirectory **[basic](/samples/basic)** with more SpecScript examples.

Running SpecScript on a directory show the commands that are available in that directory.

```shell ignore
cli basic
```

```
Simple SpecScript example scripts

* Available commands: 
   create-greeting   Creates a greeting and puts it in the output
   greet             Prints a greeting
   multiple-choice   Interaction example
   output            Sets test output
   simple-question   Simple interactive prompt
```

Once you know which script you want to execute, simply chain them as commands on the command line. For example, to
execute the `greet.cli` script in the `basic` directory, do:

```shell cli cd=samples
cli basic greet
```

This will give the expected output:

```output
Hello, World!
```

----------------------------------------------------------------------

## Supplying input

Some scripts take input. Use the [--help](Command%20line%20options.spec.md#--help) option to list the supported
parameters

```shell cli cd=samples
cli --help basic greet
```

```output
Prints a greeting

Options:
  --name   Your name
```

With that information we can give the script some custom input:

```shell cli cd=samples
cli basic greet --name Alice
```

This will print:

```output
Hello, Alice!
```

## Capturing output

Some SpecScript commands produce output. By default, the SpecScript cli does not print the output. Use
the [--output](Command%20line%20options.spec.md#--output) option to see it.

For example, the **[greet](/samples/basic/greet.cli)** script uses a **Print** command to show the greeting, whereas
**[create-greeting](/samples/basic/create-greeting.cli)** does not print anything but creates output to be used by
another script.

Running `create-greeting` like this will show nothing:

```shell cli cd=samples
cli basic create-greeting --name Bob
```

The output is empty:

```output
```

We will only see the output when passing the `--output` parameter, or its shortcut `-o`:

```shell cli cd=samples
cli -o basic create-greeting --name Bob
```

```output
Hello Bob!
```

<!-- TODO Document --output-json option -->