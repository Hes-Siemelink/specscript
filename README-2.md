# SpecScript

Write executable specifications that serve as both documentation and tests using Markdown and YAML.

SpecScript lets you create human-readable specifications that actually run, validate behavior, and stay up-to-date. No
more documentation that's out of sync with reality.

## Quick Example

Here's a complete API testing specification in `user-api.spec.yaml`:

```yaml file=user-api.spec.yaml
Script info:
  description: User API validation

Input parameters:
  name: User name
  language: Greeting language

POST:
  url: http://localhost:2525/greeting
  body:
    name: ${input.name}
    language: ${input.language}

Print: ${output}
```

Run it to test your API:

```shell cli
cli user-api.spec.yaml --name Alice --language English
```

This should produce the following output:

```output
Hi Alice!
```

This single file is simultaneously:

- **Documentation** of how your API works
- **Test suite** that validates the API behavior
- **Specification** that defines expected behavior
- **Runnable code** that executes real API calls

## Key Features

### Executable Documentation

SpecScript documentation IS the test suite. Every code example in specification documents literally executes during
tests. Write specifications in Markdown with embedded YAML that runs as part of your build, ensuring docs never go
stale.

### Built-in Testing

Test assertions, HTTP requests, data validation, and control flow - all in readable YAML syntax. Documentation that lies
will cause your build to fail.

### Human-Friendly Syntax

No complex programming constructs. Use simple YAML declarations that business stakeholders can read and understand.

### Quick Prototyping

Rapidly prototype APIs, test data pipelines, validate configurations, or create interactive command-line tools.

## More Examples

### Configuration Validation

First, create a configuration file:

```yaml file=config.json
{
  "database": {
    "host": "localhost",
    "port": 5432
  },
  "api": {
    "timeout": 30,
    "retries": 3
  },
  "features": {
    "authentication": true,
    "logging": true
  }
}
```

Now validate the configuration:

```yaml specscript
Code example: Configuration validation

Read file:
  resource: config.json
As: ${config}

Assert that:
- not:
    empty: ${config.database.host}
- item: ${config.features.authentication}
  equals: true

Print: Configuration validation passed
```

### Data Processing Pipeline

```yaml specscript
Code example: Data processing pipeline

GET: http://localhost:2525/items

Print: Retrieved items from API
```

### Interactive User Input

<!-- answers
Select deployment environment: development
-->

```yaml specscript
Code example: Interactive user input

Prompt:
  description: Select deployment environment
  enum:
    - development
    - staging  
    - production

Print: Deploying to ${output} environment
```

## How It Works

1. **Write specifications** in `.spec.yaml` files using YAML syntax
2. **Run them directly** with the `cli` command
3. **Embed in Markdown** for rich documentation that stays current
4. **Organize in directories** for subcommand support
5. **Chain operations** using variables and the `${output}` system

## Getting Started

### Install

```shell ignore
./gradlew build fatJar
alias cli="java -jar `pwd`/build/libs/specscript-*-full.jar"
```

### Hello World

Create `hello.spec.yaml`:

```yaml file=hello.spec.yaml
Print: Hello from SpecScript!
```

Run it:

```shell cli
cli hello.spec.yaml
```

```output
Hello from SpecScript!
```

### Explore Examples

```shell ignore
cli samples              # Interactive selection
cli samples/hello.spec.yaml    # Run specific example  
cli samples/http-server  # API server example
```

## Documentation

Complete language specification and command reference:

- **[Language Guide](specification/language/README.md)** - Core SpecScript syntax and concepts
- **[Command Reference](specification/commands/README.md)** - All available commands with examples
- **[CLI Usage](specification/cli/README.md)** - Command-line tool documentation

All documentation is executable - every code example literally runs during `./gradlew specificationTest`. This ensures
documentation cannot become outdated because it validates real behavior.

## Use Cases

- **API Testing** - Validate REST APIs with readable specifications
- **Configuration Validation** - Ensure configs are correct across environments
- **Data Pipeline Testing** - Test ETL processes and data transformations
- **Integration Testing** - End-to-end workflow validation
- **Prototyping** - Quickly mock APIs and interactive tools
- **Living Documentation** - Specs that stay current because they're executable tests

## Why SpecScript?

**Traditional approaches:**

- Documentation ❌ often outdated
- Tests ❌ hard to read
- Specifications ❌ separate from implementation
- Prototyping ❌ requires full development setup

**SpecScript approach:**

- Documentation ✅ always current (it's executable and tested)
- Tests ✅ readable by humans and stakeholders
- Specifications ✅ are the implementation and the tests
- Prototyping ✅ just write YAML and run

**The Magic**: Documentation that can't lie because it executes. If your examples don't work, your build fails. This is
spec-driven development where documentation validates behavior.

Everything is Markdown and YAML. Keep it simple.