# SpecScript

SpecScript is a declarative language built on YAML and Markdown. Write what you want to happen — an API call, a test, a
CLI tool, an AI tool server — and it just works. No boilerplate, no build step.

What makes it different: the same file is a script, a CLI tool, a test, an HTTP endpoint, and an MCP tool.
Documentation that can't lie, because it runs.

## See it in action

A GET request in SpecScript:

```yaml
GET: https://api.example.com/hello
```

Save it as `greet.spec.yaml`, run it with `spec greet.spec.yaml`, and you get the response. That's the whole script.

Now make it take parameters. Add an `Input schema` and SpecScript generates CLI options, help text, and interactive
prompts automatically:

```yaml
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

GET: https://api.example.com/hello?name=${name}&language=${language}
```

Run it from the command line:

```shell
spec greet.spec.yaml --name Hes --language English
# => Hi Hes!
```

You get `--help` for free:

```shell
spec --help greet.spec.yaml
# Multi-language greeting
#
# Options:
#   --name       Your name
#   --language   Select a language
```

That file is already a script and a CLI tool. But it doesn't stop there.

### Test it

Add a test to make sure it keeps working:

```yaml
Tests:

  Greets in English:
    GET: https://api.example.com/hello?name=Hes&language=English
    Expected output: Hi Hes!
```

Run with `spec --test` and it verifies the API behaves as documented.

### Serve it

Expose the same script as an HTTP endpoint:

```yaml
Http server:
  port: 8080
  endpoints:
    /hello:
      get:
        script: greet.spec.yaml
```

Query parameters are passed as input to the script automatically.

### Give it to an AI agent

Or expose it as an MCP tool — the emerging standard for connecting AI assistants to tools:

```yaml
Mcp server:
  name: greeting-tools
  tools:
    greet:
      script: greet.spec.yaml
```

SpecScript derives the tool's description and input schema from the script's `Script info` and `Input schema`. No
duplication needed.

One file. Five roles: a script, a CLI tool, a test, an HTTP endpoint, and an AI tool. That's the idea.

## Five levels of leverage

Each level builds on the previous one. Stop wherever you want — every level is useful on its own.

| Level | What you do                     | What you get                                                              |
|-------|---------------------------------|---------------------------------------------------------------------------|
| 1     | Write a YAML script             | Run API calls without curl flags, Postman tabs, or project boilerplate    |
| 2     | Add `Input schema`              | Instant CLI with `--help`, options, and interactive prompts — zero effort |
| 3     | Add `Tests`                     | Repeatable, readable verification that your API still works               |
| 4     | Reference from `Http server`    | Serve the same logic as an API endpoint for machine interop               |
| 5     | Reference from `Mcp server`     | Expose it as a tool that AI agents can discover and call                  |

You don't rewrite anything between levels. The same script file gains new capabilities just by being referenced from a
new context.

---

# It's just YAML

Commands are YAML keys that start with a capital letter. A script is a sequence of commands, top to bottom:

```yaml
Print: Hello from SpecScript!
```

Since YAML doesn't allow duplicate keys, use `---` to separate commands:

```yaml
Print: Hello
---
Print: Hello again!
```

## Variables

Variables use `${...}` syntax with dot and bracket notation for nested data:

```yaml
${var}:
  name: my variable
  content:
    a: one
    b: two

Print: ${var.content}
```

Every command stores its result in `${output}`. Capture it with `As` before the next command overwrites it:

```yaml
GET: https://api.example.com/hello
As: ${result}

Print:
  The result was: ${result}
```

## Control flow

```yaml
If:
  item: this
  equals: that
  then:
    Print: I'm confused!
```

```yaml
For each:
  ${name} in:
    - Alice
    - Bob
    - Carol
  Print: Hello ${name}!
```

---

# HTTP as code

Write HTTP requests as YAML — no curl flags to remember, no Postman tabs to lose:

```yaml
GET: https://api.example.com/items
```

```yaml
POST:
  url: https://api.example.com/greeting
  body:
    name: Hes
    language: Dutch
```

Set shared configuration with `Http request defaults`:

```yaml
Http request defaults:
  url: https://api.example.com
  headers:
    Authorization: Bearer ${token}

GET: /items
---
POST:
  path: /greeting
  body:
    name: Hes
    language: Dutch
```

For prototyping, spin up an HTTP server:

```yaml
Http server:
  port: 8080
  endpoints:
    /hello:
      get:
        script:
          Output: Hello from SpecScript!
```

---

# Testing built in

Tests are just more YAML. Run them with `spec --test`:

```yaml
Before all tests:
  Http request defaults:
    url: https://api.example.com

Tests:

  Items are returned:
    GET: /items
    Expected output: [ 1, 2, 3 ]

  Greets correctly:
    POST:
      path: /greeting
      body:
        name: Alice
        language: English
    Expected output: Hi Alice!

  Status check:
    Assert that:
      item: one
      in: [ one, two, three ]
```

This is where SpecScript's design pays off. The test reads like a specification of how the API should behave — because
it is one. Give this file to a product manager and they can review it. Give it to a developer and they can run it. Give
it to CI and it verifies the system works.

---

# Executable documentation

SpecScript also runs inside Markdown. Write a `.spec.md` file with YAML code blocks, and the prose is documentation
while the code blocks are executable:

    ## Greeting API

    The greeting endpoint accepts a name and language:

    ```yaml specscript
    POST:
      url: https://api.example.com/greeting
      body:
        name: World
        language: English
    Expected output: Hi World!
    ```

Run `spec --test` and every code block executes as a test. If someone changes the API and the documentation becomes
wrong, the test fails. Documentation that lies breaks the build.

SpecScript's own specification — 440+ tests across 80+ Markdown files — is written this way. The documentation IS the
test suite.

---

# MCP servers

Define AI tool servers using the Model Context Protocol — in YAML, without writing application code:

```yaml
Mcp server:
  name: my-tools
  port: 8090
  tools:
    greet:
      description: Greet someone
      inputSchema:
        properties:
          name:
            type: string
            description: Who to greet
      script:
        Output: Hello ${input.name}!
```

The `Input schema` from any SpecScript file automatically becomes the MCP tool's input schema. A directory of scripts
becomes a suite of tools an AI agent can call.

---

# Instant CLI

Every SpecScript file is already a command-line tool. Add `Input schema` for options and help text.

Organize scripts in directories and you get subcommands:

```shell
spec my-tools
# my-tools
#
# Available commands:
#   deploy    Deploy to environment
#   status    Check service status
#   migrate   Run database migrations
```

```shell
spec my-tools deploy --env staging
```

Build interactive prompts with a few lines of YAML:

```yaml
Prompt:
  description: Select a language
  enum:
    - English
    - Spanish
    - Dutch

Output: You selected ${output}
```

---

# Documentation

The complete language is documented in the **[specification](specification)** directory — and every example in it runs as
a test:

* [Language](specification/language/README.md) — variables, commands, control flow, packages
* [Command reference](specification/commands/README.md) — all 80+ commands with runnable examples
* [CLI](specification/cli/README.md) — running scripts, options, subcommands

---

# Get started

## Install

Copy `spec` and `specscript.conf` into your project. The wrapper script downloads the right version and runs it:

    ./spec

For the latest versions, see [SpecScript Releases](https://github.com/Hes-Siemelink/specscript/releases) on GitHub.

## Build from source

The reference implementation is in **Kotlin**. Install a current JDK and build with Gradle:

```shell
./gradlew build
alias spec="java -jar `pwd`/build/libs/specscript-*-full.jar"
```

Run the hello world example:

```shell
spec specification/hello-world.spec.yaml
```

There is also an incubating **[TypeScript](typescript)** implementation, generated by AI agents working from the
specification — a test of whether the spec is clear enough for both humans and machines.

## Explore

There are more examples in the **[samples](samples)** directory. Explore them interactively:

```shell
spec samples
spec --interactive samples/spotify
```
