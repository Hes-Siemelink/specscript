# SpecScript

Spec your projects the human _and_ AI-friendly way using Markdown and Yaml.

Use SpecScript to quickly prototype and test specifications. Sprinkle in some user interaction.

As-code, but without the complexity of actual code.

## Example

Get a flavor of SpecScript with this example file `greetings.spec.yaml`:

```yaml file=greetings.spec.yaml
Script info: Multi-language greeting

Input parameters:
  name: Your name
  language:
    description: Select a language
    enum:
      - English
      - Spanish
      - Dutch

POST:
  url: http://localhost:2525/greeting
  body:
    name: ${input.name}
    language: ${input.language}
```

Run the script with this command:

<!-- answers
Your name: Hes
Select a language: English
-->

```shell cli
./spec greetings.spec.yaml --name Hes --language English
```

When running it, we get prompted for input before a POST request is made to the server. The greeting that we get back is
printed.

```output
Hi Hes!
```

You can specify the parameters as arguments. Find out what to pass with the `--help` option:

```shell cli
./spec --help greetings.spec.yaml
```

Will print:

```output
Multi-language greeting

Options:
  --name       Your name
  --language   Select a language
```

We can call the example again with the parameters filled in:

```shell cli
./spec greetings.spec.yaml --name Hes --language Spanish
```

And we get the result in Spanish:

```output
¡Hola Hes!
```

# Documentation

All of SpecScript is defined in the **[specification](specification)** directory.

* [CLI](specification/cli/README.md) defines the `cli` shell command
* [Language](specification/language/README.md) defines the structure of the SpecScript scripting language
* [Command reference](specification/commands/README.md) defines all commands with descriptions, code examples and tests.

## SpecScript versions

For the latest versions, check the [SpecScript Releases](https://github.com/Hes-Siemelink/specscript/releases) page on
GitHub.

## Running SpecScript

Run SpecScript by invoking the wrapper script:

    ./spec

This will download the version of SpecScript specified in [specscript.conf](specscript.conf) and run it.

You can use the `spec` wrapper script in your project by copying the following files:

    spec
    specscriptr.conf

# Build form source

The SpecScript implementation is in Kotlin.

Install a current JDK, and build with Gradle:

```shell ignore
./gradlew build
alias spec="java -jar `pwd`/build/libs/specscript-*-full.jar"
```

This will run the "Hello world" example:

```shell ignore
spec samples/hello.spec.yaml
```

See [Running SpecScript files](specification/cli/Running%20SpecScript%20files.spec.md) for more information on the `cli`
command.

## Explore

There are more examples in the **[samples](samples)** directory - check them out!

Explore them all with the command:

```shell ignore
./spec samples
```

The following example will provide an interactive experience and connect to the Spotify API:

```shell ignore
./spec samples/spotify
```

When connecting to Spotify for the first time, the script will ask you for your login credentials: App Client ID and
Client secret -- you should already have those.

# Highlight Reel

SpecScript has two main ideas:

1. Everything is Markdown or Yaml.
2. Keep it simple.

## Hello world

This is the simplest SpecScript progam, **[hello.spec.yaml](samples/hello.spec.yaml)**:

```yaml file=hello.spec.yaml
Print: Hello from SpecScript!
```

Invoke it with

```shell cli
./spec hello.spec.yaml
```

And it will print the expected message:

```output
Hello from SpecScript!
```

## HTTP requests as code

Tired of remembering the exact curl syntax or forgetting which tab had that request that worked in Postman?

Simply write your **[GET](specification/commands/core/http/GET.spec.md)** request as-code with SpecScript:

```yaml specscript
GET: http://localhost:2525/greetings
```

Here's a **[POST](specification/commands/core/http/POST.spec.md)**:

```yaml specscript
POST:
  url: http://localhost:2525
  path: /greeting
  body:
    name: Hes
    language: Dutch
```

## Define input

Define all command-line options in Yaml. Take this file `simple-options.spec.yaml`

```yaml file=simple-options.spec.yaml
Script info:
  description: Call Acme

Input parameters:
  user: Username
  language: Preferred language
```

This will automatically generate a command description and command line options:

```shell cli
./spec --help simple-options.spec.yaml
```

```output
Call Acme

Options:
  --user       Username
  --language   Preferred language
```

## Input options

SpecScript allows you to specify the type and format
of [input parameters](specification/commands/core/script-info/Input%20parameters.spec.md). Here's an example file
`input-options.spec.yaml`

```yaml file=input-options.spec.yaml
Script info:
  description: Different input options

Input parameters:
  user:
    description: Username
    short option: u
  password:
    description: Password
    secret: true
    short option: p
```

```shell cli
./spec --help input-options.spec.yaml
```

```output
Different input options

Options:
  --user, -u   Username
  --password, -p   Password
```

## Subcommand support

Easily provide subcommand support by organizing your cli files in directories.

For example, to run the greeting example from the **[samples](samples)** directory, you can write

```shell cli cd=.
./spec samples basic greet
```

with output:

```
Hello, World!
```

SpecScript will show the commands if you pass a directory.

For example:

```shell cli cd=.
./spec samples
```

will print the info message:

```
samples has several subcommands.

Available commands:
  basic         Simple SpecScript example scripts
  digitalai     Interact with Digital.ai products and services.
  hello         Hello
  http-server   Use SpecScript to run a web server
  programming   Programming examples in SpecScript
  spotify       Spotify API examples

```

## User interaction

Easily construct [user prompts](specification/commands/core/user-interaction/Prompt.spec.md) with SpecScript.

Here's an example of how to ask the user to pick something from a list, in a file called `prompt.spec.yaml`:

```yaml file=prompt.spec.yaml 
Prompt:
  description: Select a language
  enum:
    - English
    - Spanish
    - Dutch

Output:
  You selected: ${output}
```

You will be presented with an interactive selector when running it:

```shell cli
./spec prompt.spec.yaml
```

<!-- answers
Select a language: English
-->

```output
? Select a language 
 ❯ ◉ English
   ◯ Spanish
   ◯ Dutch

You selected: English
```

## Variables

Define [variables](specification/language/Variables.spec.md) in `${...}` syntax and pick and choose content using the
path notation.

```yaml specscript
Code example: Define a variable

${var}:
  name: my variable
  content:
    a: one
    b: two

Print: ${var.content}
```

will print

```output
a: one
b: two
```

## The output variable

The result of a command is always stored in the variable `${output}`.

This makes it easy to pick up in a subsequent command

For example

```yaml specscript
Code example: Using the output variable

GET: http://localhost:2525/hello

Print: ${output}
```

Will print the output of the GET request:

```output
Hello from SpecScript!
```

Some commands work directly with the output variable. This helps in having a more declarative and readable script. For
example, you don;t need to pass the `${output}` variable to the **[Expected output
](specification/commands/core/testing/Expected%20output.spec.md)** command.

```yaml specscript
Code example: Implicit output variable

GET: http://localhost:2525/hello

Expected output: Hello from SpecScript!
```

If you are going to use the output variable later on, best practice is to assign it to a named variable using **[As
](specification/commands/core/variables/As.spec.md)**.

```yaml specscript
Code example: Assign output to a named variable

GET: http://localhost:2525/hello
As: ${result}

Print:
  The result of GET /hello was: ${result}
```

## Http Server

For quick API prototyping, SpecScript will run an [HTTP server](specification/commands/core/http/Http%20server.spec.md)
for you. Define some endpoints and back them by SpecScript Yaml scripts:

```yaml specscript
Code example: Running an HTTP server

Http server:
  port: 2525
  endpoints:
    /hello-example:
      get:
        script:
          Output: Hello from SpecScript!
```

Take a look at the [sample server](samples/http-server/sample-server/sample-server.spec.yaml) that serves all requests
from the SpecScript documentation and test suite.

## If statement

SpecScript supports various programming logic constructs, like 'if', 'repeat', 'for each'

This is what an **[If](specification/commands/core/control-flow/If.spec.md)** statement looks like:

```yaml specscript
Code example: If statement

If:
  item: this
  equals: that
  then:
    Print: I'm confused!
```

## For each

With **[For each](specification/commands/core/control-flow/For%20each.spec.md)** you can loop over collections and do
stuff.

```yaml specscript
Code example: For each statement

For each:
  ${name} in:
    - Alice
    - Bob
    - Carol
  Print: Hello ${name}!
```

Output:

```output
Hello Alice!
Hello Bob!
Hello Carol!
```

You can use **For each**
to [transform a list](specification/commands/core/control-flow/For%20each.spec.md#transform-a-list) into something else,
like the `map()` function in some programming languages.

```yaml specscript
Code example: For each to transform a list

For each:
  ${name} in:
    - Alice
    - Bob
    - Carol
  Output: Hello ${name}!

Expected output:
  - Hello Alice!
  - Hello Bob!
  - Hello Carol!
```

## Testing in SpecScript

Writing tests in SpecScript is straightforward:

```yaml specscript
Test case: A simple test case

Assert that:
  item: one
  in: [ one, two, three ]
```

In fact, all tests for the SpecScript language and commands are written in SpecScript itself and can be found in the
**[specification](specification)** directory, in the `tests` subfolders. For example, take a look at
the [tests for assertions](specification/commands/core/testing/tests/Assert%20tests.spec.yaml)

## Documenting SpecScript

All documentation can be found in the **[specification](specification)** directory.

SpecScript documentation is in Markdown and contains runnable code that is run as part of the test suite.

Here's an example of SpecScript documentation:

    ## Code examples
    
    The following code prints a message:
    
    ```yaml specscript
    Print: Hello from SpecScript!
    ```

You can do 'Spec-driven development' with SpecScript. For new features, write the documentation first, then run it.
Since you haven't implemented anything yet, the test suite will fail. Then write the implementation, and once the tests
are green, you're done!

