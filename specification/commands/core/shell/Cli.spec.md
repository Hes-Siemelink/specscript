# Command: Cli

With **Cli** you execute the SpecScript command without popping into a new shell. This is mainly useful to document and
test the behavior of the `cli` command line interface.

| Input  | Supported    |
|--------|--------------|
| Scalar | yes          |
| List   | auto-iterate |
| Object | no           |

[Cli.schema.yaml](schema/Cli.schema.yaml)

## Basic usage

Just pass your CLI arguments as a string to run a single CLI command. Here we use it to show the SpecScript help:

```yaml specscript
Code example: Execute SpecScript command

Cli: --help

Expected console output: |
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

## Specifying the working dir

The default working directory is `SCRIPT_HOME` — the directory containing the script being executed.

Use the long format to specify a different working directory. The [cli-example](cli-example) directory next to this spec
contains two small scripts.

```yaml specscript
Code example: Cli in a different directory

Cli:
  command: spec .
  cd: cli-example

Expected console output: |
  Example commands

  Available commands:
    count   Counts to three
    greet   Prints a greeting
```