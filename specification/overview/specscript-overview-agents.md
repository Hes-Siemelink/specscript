---
status: draft
ai-generated: true
complete-garbage: false
human-edited: false
---

# SpecScript -- Overview for AI Coding Agents

SpecScript replaces shell scripts, small Python/Node utilities, and boilerplate HTTP glue with a declarative YAML format
that is easy for both humans and AI to read, write, and verify. Because it is plain YAML, agents can generate correct
scripts without worrying about syntax edge cases, escaping, or language-specific idioms.

## What is SpecScript?

SpecScript is a declarative scripting language built on YAML. Scripts are `.spec.yaml` files containing a sequence of
commands executed top-to-bottom. There is no custom parser — it's standard YAML with a `${variable}` interpolation layer
on top. Commands start with a capital letter.

```yaml specscript
Code example: Simple GET request

GET: http://localhost:2525/hello

Expected output: Hello from SpecScript!
```

SpecScript also supports executable Markdown (`.spec.md`) where YAML code blocks serve as both documentation and
runnable code. This document focuses on `.spec.yaml` scripts, which is what agents typically write.

## Installation

Copy `spec` and `specscript.conf` into your project. The `spec` wrapper script downloads the SpecScript version
specified in `specscript.conf` and runs it. No other installation needed.

To build from source:

```bash
./gradlew build fatJar
alias spec="java -jar `pwd`/build/libs/specscript-*-full.jar"
```

Requires JDK 21+.

## CLI usage

```bash
spec script.spec.yaml                  # Run a script
spec script.spec.yaml --name Alice     # Pass input parameters
spec --help script.spec.yaml           # Show script description and options
spec --output script.spec.yaml         # Print script output as YAML
spec --output-json script.spec.yaml    # Print script output as JSON
spec --test tests/                     # Run all tests in a directory
spec directory/                        # List available scripts in directory
spec directory subcommand              # Run a specific script from directory
```

The `.spec.yaml` extension can be omitted: `spec hello` is equivalent to `spec hello.spec.yaml`.

See [Command line options](../cli/Command%20line%20options.spec.md) for full details.

## Core syntax

### Commands and the `---` separator

YAML keys starting with a capital letter are commands. Since YAML forbids duplicate keys, use `---` to repeat commands
or [Do](../commands/core/control-flow/Do.spec.md) for lists of commands inside nested structures:

```yaml specscript
Code example: Command sequence with document separators

Print: Hello
---
Print: Hello again!
```

### Variables and output capture

Variables use `${...}` syntax with JavaScript-like path notation. Every command stores its result in `${output}`.
Capture it with [As](../commands/core/variables/As.spec.md) before the next command overwrites it:

```yaml specscript
Code example: Variables, path notation, and output capture

${book}:
  title: A Small History
  chapters:
    - title: Introduction
      pages: 6

GET: http://localhost:2525/items
As: ${items}

Assert that:
  - item: ${book.chapters[0].title}
    equals: Introduction
  - item: ${items}
    equals: [ 1, 2, 3 ]
```

Note: `${varname}` in `As:` is a write target, not a dereference. See [Variables](../language/Variables.spec.md) for
full details.

### Input schema

Define script inputs using a JSON Schema subset. This auto-generates CLI `--help` output, interactive prompts, and MCP
tool schemas. Input values are available via `${input.name}` and as top-level variables like `${name}`:

```yaml
Script info: Personalized greeting

Input schema:
  type: object
  properties:
    name:
      description: Your name
      default: World
    language:
      description: Greeting language
      enum: [ English, Spanish, Dutch ]
  required:
    - name

Output: Hello ${input.name}!
```

See [Input schema](../commands/core/script-info/Input%20schema.spec.md) for the full specification.

## HTTP commands

[GET](../commands/core/http/GET.spec.md), [POST](../commands/core/http/POST.spec.md),
[PUT](../commands/core/http/PUT.spec.md), [PATCH](../commands/core/http/PATCH.spec.md),
[DELETE](../commands/core/http/DELETE.spec.md) are built-in. Use
[Http request defaults](../commands/core/http/Http%20request%20defaults.spec.md) to set shared configuration like base
URL, headers, and authentication:

```yaml specscript
Code example: HTTP requests with defaults and POST

Http request defaults:
  url: http://localhost:2525

GET: /items
As: ${items}
---
POST:
  path: /greeting
  body:
    name: Alice
    language: English
As: ${greeting}

Assert that:
  - item: ${items}
    equals: [ 1, 2, 3 ]
  - item: ${greeting}
    equals: Hi Alice!
```

## Control flow

[For each](../commands/core/control-flow/For%20each.spec.md) uses a `${var} in:` syntax to declare the loop variable:

```yaml specscript
Code example: For each loop

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

Other control flow: [If](../commands/core/control-flow/If.spec.md),
[When](../commands/core/control-flow/When.spec.md) (first-match only),
[Repeat](../commands/core/control-flow/Repeat.spec.md), [Do](../commands/core/control-flow/Do.spec.md).

## Testing

Tests only execute with `spec --test`. [Before tests](../commands/core/testing/Before%20tests.spec.md) and
[After tests](../commands/core/testing/After%20tests.spec.md) run once per file and share context with tests.

```yaml specscript
Code example: Test structure

Before tests:
  Http request defaults:
    url: http://localhost:2525

Tests:

  Items are returned:
    GET: /items
    Expected output: [ 1, 2, 3 ]

  Hello endpoint works:
    GET: /hello
    Expected output: Hello from SpecScript!

After tests:
  Print: Cleanup done
```

Assertions: [Assert that](../commands/core/testing/Assert%20that.spec.md) (conditions, negation, empty checks),
[Expected output](../commands/core/testing/Expected%20output.spec.md) (shorthand for `${output}` equality),
[Expected error](../commands/core/testing/Expected%20error.spec.md).

## MCP servers

Define Model Context Protocol servers with tools, resources, and prompts — all backed by SpecScript. External scripts
referenced in `script:` can define `Input schema`, which is automatically derived as the tool's `inputSchema`:

```yaml specscript
Code example: MCP server with tools

Mcp server:
  name: agent-overview-server
  version: "1.0.0"
  port: 8097
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

```yaml specscript
Code example: Calling an MCP tool

Mcp tool call:
  tool: greet
  input:
    name: Alice
  server:
    url: "http://localhost:8097/mcp"

Expected output: Hello Alice!
```

<!-- yaml specscript
Mcp server:
  name: agent-overview-server
  stop: true
-->

Transports: `HTTP` (default, streaming), `SSE` (legacy), `STDIO`. See
[Mcp server](../commands/ai/mcp/Mcp%20server.spec.md) for full details.

## Other capabilities

- **Error handling**: [Error](../commands/core/errors/Error.spec.md),
  [On error](../commands/core/errors/On%20error.spec.md) — catch errors with `${error.message}`
- **Shell**: [Shell](../commands/core/shell/Shell.spec.md) — run shell commands with `cd`, `env`, `capture output`
- **Files**: [Read file](../commands/core/files/Read%20file.spec.md),
  [Write file](../commands/core/files/Write%20file.spec.md),
  [Run script](../commands/core/files/Run%20script.spec.md) (`resource:` = relative to script, `file:` = relative to
  working directory)
- **Scripts as commands**:
  [SpecScript files as commands](../commands/core/files/SpecScript%20files%20as%20commands.spec.md) — organize scripts
  in directories for automatic subcommand navigation
- **User interaction**: [Prompt](../commands/core/user-interaction/Prompt.spec.md),
  [Confirm](../commands/core/user-interaction/Confirm.spec.md) — use `--interactive` to enable prompts
- **Data manipulation**: [Find](../commands/core/data-manipulation/Find.spec.md),
  [Sort](../commands/core/data-manipulation/Sort.spec.md), [Add](../commands/core/data-manipulation/Add.spec.md),
  [Size](../commands/core/data-manipulation/Size.spec.md),
  [Fields](../commands/core/data-manipulation/Fields.spec.md) and more

## Built-in variables

- `${output}` — result of the last command
- `${input}` — script input parameters (from Input schema)
- `${error}` — error details (inside On error blocks)
- `${item}` — current item in For each (when no explicit variable)
- `${SCRIPT_HOME}` — directory containing the current script
- `${SCRIPT_TEMP_DIR}` — temporary directory for the current script

## YAML pitfalls to remember

1. **No duplicate keys** — use `---` separators or `Do:` lists
2. **Strings that look like numbers** — quote them: `version: "1.0.0"` not `version: 1.0.0`
3. **Colons in values** — quote strings containing `: ` (space after colon)
4. **Boolean traps** — `yes`, `no`, `true`, `false` are parsed as booleans; quote if you mean strings

## Command reference

For the full list of all 56+ commands with descriptions and examples, see the
[SpecScript Command Reference](../commands/README.md).
