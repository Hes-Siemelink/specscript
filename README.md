# SpecScript

Write a GET request in one line. Make it a CLI with a handful more lines. Add endpoints and it's a server. No
boilerplate, no build step.

SpecScript is for people who want to get things done -- call an API, build a quick CLI tool, prototype a server --
without setting up a project first.

SpecScript is built on Yaml and Markdown. Simple to read, simple to write. Say what you want to do in a declarative
style and you're in business.

## See it in action

This is how you do a GET request in SpecScript:

```yaml temp-file=greet.spec.yaml
GET: http://localhost:2525/hello
```

It's that simple. Save it as `greet.spec.yaml`, run it like this:

```cli cd=${SCRIPT_TEMP_DIR}
spec greet.spec.yaml
```

And you get the response:

```output
Hello from SpecScript!
```

Now let's make something more interesting — a script that takes input and calls an API.

Here's a file called `greetings.spec.yaml`:

```yaml temp-file=greetings.spec.yaml
Script info: Multi-language greeting

Input schema:
  type: object
  properties:
    name:
      description: Your name
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

Run it:

<!-- answers
Your name: Hes
Select a language: English
-->

```cli cd=${SCRIPT_TEMP_DIR}
spec greetings.spec.yaml --name Hes --language English
```

```output
Hi Hes!
```

That's it. `Input schema` defines your CLI options. `POST` makes the HTTP request. The result is printed automatically.

You get `--help` for free:

```cli cd=${SCRIPT_TEMP_DIR}
spec --help greetings.spec.yaml
```

```output
Multi-language greeting

Options:
  --name       Your name
  --language   Select a language
```

---

# It's just Yaml

No special syntax — it's Yaml all the way down. Here's the obligatory hello world,
**[hello.spec.yaml](specification/hello-world.spec.yaml)**:

```yaml temp-file=hello.spec.yaml
Print: Hello from SpecScript!
```

```cli cd=${SCRIPT_TEMP_DIR}
spec hello.spec.yaml
```

```output
Hello from SpecScript!
```

Commands are Yaml keys that start with a capital letter. A script is a sequence of commands, top to bottom.

## Variables

Define [variables](specification/language/Variables.spec.md) with `${...}` syntax. Navigate nested data with dot and
bracket notation.

```yaml specscript
Code example: Define a variable

${var}:
  name: my variable
  content:
    a: one
    b: two

Print: ${var.content}
```

```output
a: one
b: two
```

Every command stores its result in `${output}`, which you can capture with
**[As](specification/commands/core/variables/As.spec.md)**:

```yaml specscript
Code example: Assign output to a named variable

GET: http://localhost:2525/hello
As: ${result}

Print:
  The result of GET /hello was: ${result}
```

## Control flow

```yaml specscript
Code example: If statement

If:
  item: this
  equals: that
  then:
    Print: I'm confused!
```

```yaml specscript
Code example: For each statement

For each:
  ${name} in:
    - Alice
    - Bob
    - Carol
  Print: Hello ${name}!
```

```output
Hello Alice!
Hello Bob!
Hello Carol!
```

---

# HTTP as code

Tired of remembering curl syntax or losing that Postman tab that worked?

Write your **[GET](specification/commands/core/http/GET.spec.md)** request as Yaml:

```yaml specscript
GET: http://localhost:2525/greetings
```

A **[POST](specification/commands/core/http/POST.spec.md)** with a body:

```yaml specscript
POST:
  url: http://localhost:2525
  path: /greeting
  body:
    name: Hes
    language: Dutch
```

For prototyping, SpecScript can run an [HTTP server](specification/commands/core/http/Http%20server.spec.md) too:

```yaml specscript
Code example: Running an HTTP server

Http server:
  name: sample-server
  port: 2525
  endpoints:
    /hello-example:
      get:
        script:
          Output: Hello from SpecScript!
```

---

# Instant CLI

Every SpecScript file is already a command-line tool. Add an `Input schema` and you get options, help text, and
interactive prompts automatically.

```yaml temp-file=simple-options.spec.yaml
Script info:
  description: Call Acme

Input schema:
  type: object
  properties:
    user:
      description: Username
    language:
      description: Preferred language
```

```cli cd=${SCRIPT_TEMP_DIR}
spec --help simple-options.spec.yaml
```

```output
Call Acme

Options:
  --user       Username
  --language   Preferred language
```

## Directories and Subcommands

SpecScript will show the commands if you pass a directory.

```cli
spec specification/code-examples
```

will print the info message:

```output
Reference code used by specification tests

Available commands:
  basic           Simple SpecScript example scripts
  sample-server   Interact with the SpecScript Sample Server
```

By organizing script files in directories you get subcommands.

For example, to run `greet.spec.yaml` in the **[basic](specification/code-examples/basic)**
directory, you can write

```cli cd=specification/code-examples
spec basic greet
```

with output:

```output
Hello, World!
```

## User interaction

Build [interactive prompts](specification/commands/core/user-interaction/Prompt.spec.md) with a few lines of Yaml:

```yaml temp-file=prompt.spec.yaml 
Prompt:
  description: Select a language
  enum:
    - English
    - Spanish
    - Dutch

Output:
  You selected: ${output}
```

<!-- answers
Select a language: English
-->

```cli cd=${SCRIPT_TEMP_DIR}
spec prompt.spec.yaml
```

```output
? Select a language 
 ❯ ◉ English
   ◯ Spanish
   ◯ Dutch

You selected: English
```

---

# Testing built in

Writing tests in SpecScript is just more Yaml:

```yaml specscript
Tests:

  A simple test:
    Output: Hello world
    Expected output: Hello world

  Another test:
    Assert that:
      item: one
      in: [ one, two, three ]
```

---

# Documentation

There is a lot more to SpecScript than what's shown here. The complete language is documented in the
**[specification](specification)** directory:

* [Language](specification/language/README.md) -- how variables, commands, and control flow work
* [Command reference](specification/commands/README.md) -- all commands, each with runnable examples
* [CLI](specification/cli/README.md) -- running scripts, options, and subcommands

---

# The spec IS the test suite

This is the part where it gets meta.

The specification documents are Markdown files with embedded Yaml examples -- and every example actually runs as a test:

    ## Code examples
    
    The following code prints a message:
    
    ```yaml specscript
    Print: Hello from SpecScript!
    ```

The ` ```yaml specscript ``` ` code block is executable. When you run `./gradlew specificationTest`, all examples in the
specification execute and must pass. Documentation that lies breaks the build.

SpecScript is specified in SpecScript. The documentation _is_ the test suite, and the test suite _is_ the documentation.
Change the spec, run the tests; if they pass, the documentation is correct by definition.

---

# Get started

## Running SpecScript

Run SpecScript using the wrapper script:

    ./spec

This downloads the version specified in [specscript.conf](specscript.conf) and runs it. Copy `spec` and
`specscript.conf` to your own project to use SpecScript there.

For the latest versions, see [SpecScript Releases](https://github.com/Hes-Siemelink/specscript/releases) on GitHub.

## Build from source

The reference implementation is in **Kotlin**. Install a current JDK and build with Gradle:

```shell ignore
./gradlew build
alias spec="java -jar `pwd`/build/libs/specscript-*-full.jar"
```

Run the hello world example:

```shell ignore
spec specification/hello-world.spec.yaml
```

There is also an incubating **[TypeScript](typescript)** implementation. It was generated by AI agents working from the
specification -- a good test of whether the spec is clear enough for both humans and machines. Try it with:

```shell ignore
cd typescript && npm install && npm start -- ../specification/hello-world.spec.yaml
```

## Explore

There are more examples in the **[samples](samples)** directory. Explore them interactively:

```shell ignore
spec samples
```

Use the `--interactive` (or `-i`) flag to run any script in interactive mode -- SpecScript will prompt for any input it
needs:

```shell ignore
spec --interactive samples
```

Try the Spotify example for a fully interactive experience connecting to a real API:

```shell ignore
spec -i samples/spotify
```
