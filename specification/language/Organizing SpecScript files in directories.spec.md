## Organizing SpecScript files in directories

With one or more files in a directory, you can run the directory as a cli command. The SpecScript files will be
subcommands.

For this example we use the [my-scripts](my-scripts) directory next to this spec, which has the following structure:

```
my-scripts/
├── README.md
├── count.spec.yaml
└── greet.spec.yaml
```

Now let's pass `my-scripts` as a cli parameter. The `--help` option lists the available subcommands:

```cli
spec --help my-scripts
```

Shows:

```output
A collection of simple scripts

Available commands:
  count   Counts to three
  greet   Prints a greeting
```

Invoke the **greet** script as a subcommand:

```cli
spec my-scripts greet
```

With the expected output:

```output
Hello, World!
```

Note that it's optional to specify the `.spec.yaml` extension. The following three commands are equivalent:

```cli
spec my-scripts greet
```

```cli
spec my-scripts greet.spec.yaml
```

```cli
spec my-scripts/greet.spec.yaml
```

### Interactive command chooser

When invoking a directory without the `--help` parameter, SpecScript lets you select the command with an interactive
prompt. This is a great way to explore the commands and subcommands!

<!-- Insert gif here -->

```shell ignore
spec my-scripts
```

```
A collection of simple scripts

* Available commands: 
 > count   Counts to three
   greet   Prints a greeting
```

### Calling another script

We showed above that you can call another SpecScript file with the
**[Run script](../commands/core/files/Run%20script.spec.md)** command.

Another way is to use it as a regular command. SpecScript reads all cli files in the same directory and makes them
available as commands in the current script. While doing so, it transforms file names in "kebab-style" to "Sentence
style".

For example, suppose we have a file `create-greeting.spec.yaml`, that creates a greeting and puts it in the output:

```yaml temp-file=create-greeting.spec.yaml
Script info: Creates a greeting

Input schema:
  type: object
  properties:
    name:
      description: Your name

Output: Hello ${name}!
```

We can now call it as `Create greeting` from a script in the same directory:

```yaml specscript
Code example: Calling another cli file

Create greeting:
  name: Cray

Expected output: Hello Cray!
```

Scripts take object parameters. If you pass a list, the script will be executed for each item in the list.

```yaml specscript
Code example: Calling another cli file with a list of parameters

Create greeting:
  - name: Alice
  - name: Bob
  - name: Carol

Expected output:
  - Hello Alice!
  - Hello Bob!
  - Hello Carol!
```

## The `specscript-config.yaml` file

Each directory can have a `specscript-config.yaml` file that contains metadata about the directory.

You can give the directory a readable description, import commands from other directories, and manage http connection
data.

### Directory description

Add a `specscript-config.yaml` file to the directory to give a description to the current directory.

```yaml temp-file=specscript-config.yaml
Script info: This is an example directory
```

The information is printed when displaying help for the directory:

```cli cd=${SCRIPT_TEMP_DIR}
spec --help .
```

```output
This is an example directory

Available commands:
  create-greeting   Creates a greeting
```

If there is no `specscript-config.yaml` file, or it doesn't have a description, SpecScript will use the first sentence
of the README.md file in the directory.
<!-- TODO: Add example and test cases -->

### Hidden directory

You can hide the directory from the interactive command chooser by setting the `hidden` property to `true`.

For example take the following `specscript-config.yaml` file in the `subcommand` directory:

```yaml temp-file=subcommand/specscript-config.yaml
Script info:
  hidden: true
```

It will not show up as a subcommand when invoking `cli --help`.

### SpecScript version

You can indicate the version of the SpecScript spec that the script is using.

```yaml specscript
Script info:
  specscript-version: v0.1
```

### Importing commands from another directory

Scripts can call other scripts in the same directory as regular commands (see
[Calling another script](#calling-another-script) above). To import commands from other directories or external
packages, use the `imports` section in `specscript-config.yaml`. See **[Packages](Packages.spec.md)** for the full
import system including packages, wildcards, aliased imports and import scope.

Here is a simple example of importing from a local subdirectory. Given the file
`helper/say-something.spec.yaml`:

```yaml temp-file=helper/say-something.spec.yaml
Output: Something ${input.what}
```

Import from the local `helper` subdirectory in `specscript-config.yaml`:

```yaml temp-file=specscript-config.yaml
Script info: This is an example directory

imports:
  ./helper:
    - say-something
```

Now call it from `call-helper.spec.yaml`:

```yaml temp-file=call-helper.spec.yaml
Code example: Calling a script that was imported from another directory

Say something:
  what: funny

Expected output: Something funny
```

Run it:

```cli cd=${SCRIPT_TEMP_DIR}
spec call-helper
```

And the expected output is:

```output
Something funny
```

See **[Packages](Packages.spec.md)** for more info on the syntax, for example how to import from external packages, and
how to alias imports.

### Connections

The `specscript-config.yaml` file also contains a `connections` settings for retrieving HTTP connection credentials. See
the
**[Connect to](../commands/core/connections/Connect%20to.spec.md)** command for more details.
