# SpecScript command-line options

The names and descriptions of the global options are defined
in [specscript-command-line-options.yaml](specscript-command-line-options.yaml) and this document explains the behavior.

## Global options

When running `spec` or `spec --help`, the global options will be printed

```shell cli
spec
```

```output
SpecScript -- Create instantly runnable specs using Yaml and Markdown!

Usage:
   spec [global options] file | directory [command options]

Global options:
  --help, -h      Print help on a script or directory and does not run anything
  --output, -o    Print the output at the end of the script in Yaml format
  --output-json, -j   Print the output at the end of the script in Json format
  --interactive, -i   SpecScript may prompt for user input if it needs more information
  --debug, -d     Run in debug mode. Prints stacktraces when an error occurs.
```

### --help

The `--help` option prints help on a script or directory and then exits. No scripts are run.

For this example we run from the **[samples](/samples)** directory. It contains a directory `basic`. Let's use the
`--help` option to see what SpecScript commands it contains

```shell cli cd=samples
spec --help basic
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

Using `--help` on the **[greet](/samples/basic/greet.spec.yaml)** command will give us a description and show which
command line options it supports

```shell cli cd=samples
spec --help basic greet
```

```output
Prints a greeting

Options:
  --name   Your name
```

With that information we can call it with a parameter that is specific to that command:

```shell cli cd=samples
spec basic greet --name Alice
```

With the expected output:

```output
Hello, Alice!
```

### --output

Some SpecScript scripts will produce output. By default, the `cli` command does not print the output. You can turn it on
with the
`--output` option.

For example, the **[greet](/samples/basic/greet.spec.yaml)** script uses a **Print** command to show the greeting,
whereas
**[create-greeting](/samples/basic/create-greeting.spec.yaml)** does not print anything but creates output to be used by
another script.

Running `create-greeting` like this will show nothing

```shell cli cd=samples
spec basic create-greeting --name Bob
```

Output:

```output
```

We will see the output when passing the `--output` parameter, or its shortcut `-o`:

```shell cli cd=samples
spec -o basic create-greeting --name Bob
```

```output
Hello Bob!
```

### --output-json

To show the output in the script in Json format, use `--output-json` or the shortcut  `-j`:

```shell cli cd=samples
spec --output-json basic create-greeting --name Bob
```

```output
"Hello Bob!"
```

### --debug

Use this option to see stacktraces from the underlying runtime when an internal error occurs. This option is meant for
troubleshooting the SpecScript runtime.

For example, the file `error-in-add.spec.yaml` has an error in it that is not handled by SpecScript.

```yaml file=script-with-error.spec.yaml
GET: http:\\localhost  # Malformed URL - not caught by SpecScript runtime
```

Without debug mode you get the following error message

```shell cli
spec script-with-error.spec.yaml
```

```output
Scripting error

Caused by: java.net.URISyntaxException: Illegal character in opaque part at index 5: http:\\localhost

In script-with-error.spec.yaml:

  GET: http:\\localhost
```

With the `--debug` option you will see more of the internals. For example, the Kotlin stacktrace. This can be useful for
debugging the implementation.

```shell cli
spec --debug script-with-error.spec.yaml
```

May print something like

```
Scripting error

Caused by: java.net.URISyntaxException: Illegal character in opaque part at index 5: http:\\localhost
	at java.base/java.net.URI$Parser.fail(URI.java:2976)
	at java.base/java.net.URI$Parser.checkChars(URI.java:3147)
	at java.base/java.net.URI$Parser.parse(URI.java:3183)
	at java.base/java.net.URI.<init>(URI.java:623)
	at specscript.commands.HttpCommandsKt.processRequestWithoutBody(HttpCommands.kt:150)
	at specscript.commands.HttpCommandsKt.access$processRequestWithoutBody(HttpCommands.kt:1)
	at specscript.commands.HttpGet.execute(HttpCommands.kt:52)
	at specscript.script.CommandExecutionKt.handleCommand(CommandExecution.kt:82)
	at specscript.script.CommandExecutionKt.runSingleCommand(CommandExecution.kt:59)
	at specscript.script.CommandExecutionKt.runCommand(CommandExecution.kt:20)
	at specscript.script.Script.runScript(Script.kt:23)
	at specscript.files.CliFile.run(CliFile.kt:30)
	at specscript.cli.SpecScriptMain.invokeFile(Main.kt:88)
	at specscript.cli.SpecScriptMain.run(Main.kt:61)
	at specscript.cli.SpecScriptMain.run$default(Main.kt:39)
	at specscript.cli.SpecScriptMain$Companion.main(Main.kt:161)
	at specscript.cli.SpecScriptMain$Companion.main$default(Main.kt:151)
	at specscript.cli.MainKt.main(Main.kt:21)

In error.spec.yaml:

  GET: http:\\localhost
```

This may help the SpecScript runtime developer to diagnose the problem and fix it,