# AGENTS.md

We're in the Specscript language repo and the entire language is defined in the `specification` dir. Read the
specification to know what SpecScript is. Starting points: Main README.md, `specifciation/overview` directory, and
`specification/language/` directory.

**IMPORTANT**: before you do anything else, run the `beans prime` command and heed its output.

Use `.tmp` in this repo as a temp directory so you don't need to ask for permission to write files somewhere else.##
Project Overview

## Project Overview

SpecScript is a tool for creating human and AI-friendly specifications using Markdown and YAML. It provides a CLI tool
called `spec` that can execute `.spec.yaml` script files containing YAML specifications for HTTP requests, user
interaction, testing, and more.

The `specification/` directory contains the complete language specification written in SpecScript Markdown itself. This
includes tests written in SpecScript yaml. The `samples` directory contains SpecScript example code. The `src/`
directory contains the Kotlin implementation of the SpecScript language.

When doing a full build, both the specification tests and the unit tests run. The specification tests execute all the
code examples in the documentation, ensuring that the documentation is always accurate and up-to-date.

## Development Process

SpecScript is developed using a spec-first approach, a bit like TDD. When adding new features or commands, you should
first write the specification in the `specification/` directory with executable examples. The build will fail on these.
Then analyze the Kotlin code to make a plan, implement and make sure all tests run.

Full steps are:

1. User gives a problem statement or feature request.
2. Analyze the problem and write a proposal. Store the proposal as a plain Markdown file in the `plan/proposals`
   directory. The Markdown is not specscript yet, just a regular Markdown document describing the problem, the proposed
   solution, and any relevant details. Depending on the problem statement, this could be high level (new language
   feature), mid-level (new command), or low level (bug fix or refactoring).
3. User reviews and confirms the proposal. Do not proceed without explicit confirmation.
4. Write the spec. This is the most critical step. The spec defines the behavior and serves as documentation and tests.
   Write it in the `specification/` director. For invasive changes, put the new or heavily revised spec in
   `plan/draft-specs`.
5. User reviews and confirms the spec. Do not proceed without explicit confirmation.
6. Implement the code in Kotlin. IMPORTANT: follow existing patterns and architecture. Do not introduce new patterns or
   architectural styles without explicit confirmation. Put any suggestions for improvements in `plan/agent-ideas.md` as
   conciser one-liners to be reviewed later
7. User reviews and confirms the spec. Do not proceed without explicit confirmation.
8. Prepare commit according to Git commit rules below. User will review and push

## Build and Development Commands

### Build the project

```bash
./gradlew build
```

### Run tests

```bash
./gradlew test                    # Unit tests
./gradlew specificationTest       # Specification tests (440+ tests)
./gradlew check                   # All tests including specification tests
```

### Create executable

```bash
./gradlew build fatJar
# Fat JAR (self-contained)
alias spec="java -jar `pwd`/build/libs/specscript-*-full.jar"
```

### Run examples

```bash
spec samples/hello.spec.yaml
spec samples                       # Interactive selection
```

### Release process

```bash
./gradlew githubRelease          # Create GitHub release (includes both thin and fat JARs)
```

### Library publishing

```bash
./gradlew publishToMavenLocal    # Publish to local Maven repository
```

Published artifacts:

- `hes.specscript:specscript:0.6.0-SNAPSHOT` - Main library JAR
- `hes.specscript:specscript:0.6.0-SNAPSHOT:sources` - Sources JAR
- `hes.specscript:specscript:0.6.0-SNAPSHOT:javadoc` - Javadoc JAR

## Architecture

### Core Components

- **CLI Entry Point**: `src/main/kotlin/specscript/cli/Main.kt` - Main application entry point
- **Language Engine**: `src/main/kotlin/specscript/language/` - Core language processing and command execution
- **Commands**: `src/main/kotlin/specscript/commands/` - Implementation of all script commands (HTTP, testing, control
  flow, etc.)
    - Commands use paths like `"core/testing"` to reference specification files
    - 56+ command implementations organized by functionality
- **File Handling**: `src/main/kotlin/specscript/files/` - .spec.yaml file parsing and management
- **Utilities**: `src/main/kotlin/specscript/util/` - JSON/YAML processing, I/O utilities

### Architectural Principles

- The project is split into `specification` (the language specification) and `src` (the Kotlin implementation).
- Commands are implemented as singleton objects extending `specscript.language.CommandHandler`.
- The command's name and group are registered in the `CommandHandler` constructor, e.g.,
  `CommandHandler("Mcp server", "ai/mcp")`.
- The command logic is implemented in the `execute` method.

### Server Architecture Patterns

SpecScript implements server patterns for both MCP and HTTP servers with similar but not identical structures:

**Common Server Patterns:**

- Server registries using `mutableMapOf` for tracking running servers
- Lifecycle management with start/stop operations
- Context cloning for isolated script execution
- Support for both inline scripts and external file references

**MCP Server Specifics:**

- Registry keyed by server `name` (string)
- Schema: `name`, `version`, `tools`, `resources`, `prompts`, `stop`
- Uses MCP Kotlin SDK with stdio transport
- Lifecycle: explicit `stop: true` flag for shutdown

**HTTP Server Specifics:**

- Registry keyed by `port` (integer)
- Schema: `port`, `endpoints` with method handlers
- Uses Ktor with Netty for HTTP handling
- Lifecycle: separate stop command by port

**Server Alignment Opportunities:**

- Property naming consistency (both should support `name`, `version`, `stop`)
- Unified lifecycle interface implementation
- Consistent context variable handling (`input` vs `request`)
- Server registry access patterns for modular tool/endpoint definitions

### MCP Server Implementation Guidance

**Critical MCP Patterns to Follow:**

- Use name-based server registry: `servers = mutableMapOf<String, Server>()`
- Follow tools/resources/prompts structure (not endpoints like HTTP)
- Implement explicit lifecycle: `stopServer(name: String)` with `server.close()`
- Use `INPUT_VARIABLE` for consistent context handling
- Support both inline scripts and external file references

**MCP Testing Strategy:**

- MCP servers require **manual lifecycle management** in tests (not automatic like HTTP)
- Use explicit start/stop commands: `Start mcp server` / `Stop mcp server`
- NOT integrated into test framework automatic lifecycle
- This provides better control and explicit resource management

**MCP Backlog System Patterns:**

- Follow `create_ticket`, `update_ticket`, `delete_ticket`, `list_tickets` naming
- Use standard CRUD operations with proper state management
- Implement ticket states: `todo`, `doing`, `done` with validation
- Support filtering by state, assignee in list operations
- Include proper error handling for missing tickets/invalid states

**YAML Correctness for MCP:**

- All MCP tool examples must be valid YAML with proper nesting
- Use `---` separators between multiple commands (YAML constraint)
- Avoid duplicate keys - use list syntax for repetition
- Proper indentation for inputSchema properties
- Schema validation: `type`, `enum`, `description` properties must be correctly structured

### Test Structure

- **Unit Tests**: `src/tests/unit/` - Traditional unit tests
- **Specification Tests**: `src/tests/specification/` - Tests that validate the SpecScript language specifications
    - All 440+ specification tests run executable documentation
    - Uses Jackson dependencies for JSON processing in tests

### Documentation and Specifications

The `specification/` directory contains the complete language specification written in executable Markdown:

- `specification/language/` - Language syntax and features
- `specification/commands/core/` - Core command reference with examples (renamed from specscript)
- `specification/commands/ai/` - AI-related commands (MCP server, etc.)
- `specification/cli/` - CLI tool usage

All documentation includes runnable code examples that are executed as part of the test suite.

## Key Technologies

- **Kotlin 2.1.20** with JVM target 21
- **Jackson** for JSON/YAML processing
- **Ktor** for HTTP client and server functionality
- **JUnit 5 + Kotest** for testing
- **Gradle** with Kotlin DSL for build management

### Coding Conventions

- **JSON Schema:** For map-like structures, use `additionalProperties: { ... }`. Do not use `patternProperties`.
- **Kotlin:** Follow the standard Kotlin style guide.
- **Good OO / encapsulation:** Encapsulate logic within appropriate classes and provide utility functions to reduce
  client code complexity. For example, McpServer encapsulates server context management rather than exposing raw session
  key manipulation.
- **Sparse commenting:** Most code should be self-explanatory. Avoid verbose comments that describe implementation
  details. Use comments only for:
    - TODOs indicating known limitations or future improvements
    - Non-obvious business logic that cannot be expressed clearly in code
    - API documentation (KDoc) for public methods when the purpose isn't immediately clear
    - Avoid explanatory comments like "// Get the current server" when the code clearly shows
      `getCurrentServer(context)`
- **File Naming:** Specification files use spaces in their names (e.g., `Mcp server.spec.md`), which is important for
  file system operations.
- **API design over inline ceremony:** Prefer a purpose-named method that describes *what* you want over exposing
  mechanism at the call site. Null handling, type parameters, and library-specific ceremony belong behind the API. For
  example, prefer `Json.toObject(nullableMap)` over
  `Json.valueToTree<ObjectNode>(nullableMap ?: emptyMap<String, Any>())`.

## Important Notes

- The project uses "specscript" internally but is branded as "SpecScript" externally
- All specifications are executable - documentation doubles as test cases
- The `specification/` directory is included as a resource directory for runtime access
- The CLI supports interactive mode with prompts and non-interactive mode with command-line arguments
- Command implementations reference `"core/"` paths instead of legacy `"specscript/"` paths
- Two JAR artifacts are built: thin (531KB) and fat (36MB) for different deployment scenarios
- Run the tests before creating a commit

## Planning and refactoring

- Plans live in `/plan` directory. Key plans: `specscript-to-specscript-library-refactoring.md`,
  `mcp-backlog-focused-plan.md`, `server-alignment-future-plan.md`.
- When refactoring, follow a multi-phase approach. Complete and test each phase before proceeding.
- Always run `./gradlew specificationTest` before committing — the specs ARE the tests.
- Prefer thin JARs with Gradle-managed transitive dependencies over fat JARs for library consumption.
- Git is the backup system — don't create backup files in a git-controlled directory.

## Git commit rules

When committing changes to the project, follow these rules:

- **Always ask for confirmation before committing.** Do not commit autonomously.
- Use: `git commit -m "Summary" -m "content"`
- Summary:
    - Must not exceed 70 characters.
    - Write the summary as a user-focused release note item, describing the functional change or improvement from the
      perspective of a non-developer stakeholder.
    - Avoid technical or code-centric language in the summary; save those details for the content/body.
    - If a new feature is added, start the summary with the 💫 emoji. The 💫 entries tell the story at a glance.
    - For non-functional updates (refactoring, documentation, etc.), do not use any emoji.
    - For bug fixes, use exactly: `Bug fix` as the summary.
    - For invasive code structure changes without functional updates, use exactly: `Refactoring` as the summary.
    - For documentation updates, use exactly: `Documentation` as the summary.
    - For code cleanups (removing unused code, imports, fixing linter warnings, etc. without invasive refactoring), use
      exactly: `Code Cleanup` as the summary.
    - For updates to AI agent context, conversation history, distilled rules, or project meta files (such as .specstory,
      .cursor, AGENTSI.md or similar), use exactly: `AI Context` as the summary. Do not mix these with other changes.
- Content:
    - Each bullet or paragraph must be a separate `-m` argument.
    - Do not use `\n` to separate topics within a single `-m` argument.
    - Do not use backticks (`) in commit messages.
    - Do not mention test status unless there are failing tests requiring attention
    - If the code was generated by LLM, you may add the 🤖 emoji and the name of the agent in the content

## SpecScript documentation and testing philosophy

SpecScript documentation IS the test suite. Every code example in specification documents executes during
`./gradlew specificationTest`. You cannot lie in documentation — if you write it, it must work or tests fail.

The files in `specification/language/` are the authoritative reference for SpecScript syntax:

- `SpecScript Yaml Scripts.spec.md` — core language syntax and command usage
- `SpecScript Markdown Documents.spec.md` — proper structure for spec files including hidden setup/cleanup
- `Organizing SpecScript files in directories.spec.md` — file organization patterns

### Code block types

- ````yaml specscript` — **EXECUTABLE**: runs as tests during `specificationTest`
- ````yaml` — **ILLUSTRATIVE**: shows syntax without execution
- ````yaml file=filename.spec.yaml` — **FILE CREATION**: creates temporary files during test execution
- `<!-- yaml specscript -->` — **HIDDEN EXECUTABLE**: runs but not rendered in docs (setup/cleanup)

### Rules for specification documents

1. All `yaml specscript` blocks must be valid, working code
2. Use `localhost:2525` endpoints — `sample-server.spec.yaml` runs automatically during tests
3. Invalid examples must use plain `yaml`, never `yaml specscript`
4. Use sentence case for section titles
5. Multiple commands in one block require `---` separators (YAML duplicate key constraint)
6. Always run `./gradlew specificationTest` before committing

### Sample server endpoints (available during tests)

- `POST /greeting` — accepts `name` and `language`, returns formatted greeting
- `GET /items` — returns `[1, 2, 3]`
- `POST /items` — echoes back input as `Fields: ${input}`
- `GET /hello` — returns `"Hello from SpecScript!"`

### File extension rules

- **`.spec.md`**: Executable documentation with `yaml specscript` blocks
- **`.spec.yaml`**: Pure SpecScript YAML scripts
- Never use `yaml file=filename.spec.yaml` as a substitute for executable code in `.spec.md` files

## Spec-first development

Write the specification BEFORE implementing code. The spec defines the behavior; implementation follows.

1. Write `.spec.md` with executable examples defining the desired behavior
2. Optionally mock with `Output:` commands for early iteration
3. Run `./gradlew specificationTest` — if tests fail, the spec is wrong
4. Implement in Kotlin only when the spec is solid
5. Iterate: change behavior by changing the spec first

## Adding new commands

### Phase 1: Specification

1. Start with the simplest, most natural way a user would express their intent
2. Write `.spec.md` with realistic, declarative examples
3. Expand to cover edge cases and complex scenarios

### Phase 2: Technical design

1. Study similar commands for structural consistency
2. Design YAML structure and determine content type support (Value/List/Object)
3. Create `schema/CommandName.schema.yaml`

### Phase 3: Kotlin implementation

1. Create a `CommandHandler` singleton, implementing `ObjectHandler`, `DelayedResolver`, etc. as needed
2. Reuse existing classes and utilities — good OO/encapsulation
3. Register in `CommandLibrary.kt`
4. Use `ScriptContext.session` for cross-command state when appropriate

### Phase 4: Testing

1. Rapid iteration: `./gradlew build fatJar -x test -x specificationTest`
2. Validate individual spec: `spec your-spec.spec.md`
3. Full suite: `./gradlew specificationTest`

## Input handling architecture

SpecScript has two commands for defining script input: `Input parameters` (legacy) and `Input schema` (recommended).
Both populate the `${input}` variable identically.

### Input schema (preferred for new scripts)

- Uses standard JSON Schema syntax (`type: object`, `properties`, `required`)
- Implemented in `InputSchema.kt` — implements `ObjectHandler` + `DelayedResolver`
- Converts JSON Schema `properties` into `ParameterData` objects, then delegates to
  `InputParameters.populateInputVariables()` for resolution
- Only a narrow JSON Schema subset is supported: `description`, `default`, `enum`, `type` (informational),
  `condition` (SpecScript extension) per property; `type`, `properties`, `required` at top level
- `ParameterData.schema.yaml` is shared between both commands via `$ref`

### Variable resolution priority

`populateInputVariables()` resolves each property in order: existing value → default → recorded test answer →
interactive prompt → `MissingInputException`.

### `DelayedResolver` interface

Commands implementing `DelayedResolver` receive raw YAML data without variable expansion. The command handles
`${variable}` resolution itself. Both `InputParameters` and `InputSchema` use this because property definitions may
contain variable references (e.g., `condition: { item: ${input.switch} }`).

### Script metadata extraction

`Script.getScriptInfo()` scans command lists for both `Input parameters` and `Input schema` to extract metadata for CLI
`--help`, MCP tool schema derivation, and test scaffolding.

**Important**: `ScriptInfoData` does not preserve the `required` array — `deriveInputSchema()` in `McpServer.kt`
reads raw command data from `Script.commands` instead.

### MCP tool schema derivation

When an MCP tool references a script file and has no explicit `inputSchema`, `McpServer.deriveInputSchema()` loads the
script and extracts its `Input schema` (falling back to `Input parameters`). This eliminates schema duplication between
script input definitions and MCP tool definitions.

### Two independent validation systems

1. **JSON Schema validation** (networknt library) — validates command YAML structure against `schema/*.schema.yaml`
2. **SpecScript type system** (`TypeRegistry`/`TypeSpecification`) — runtime type checking for variables

These systems do not interact.

## Sensitive areas

- **`samples/basic/` directory**: Adding or removing files here breaks 3 spec files that hardcode directory listing
  output (`Organizing SpecScript files in directories.spec.md`, `Command line options.spec.md`, `Cli.spec.md`). Update
  those specs if you modify the directory contents.

## AI Assistant Response Style

I am a grumpy old European with 20 years of experience in software engineering.

To keep interactions efficient and on-topic, AI assistants MUST follow these response rules:

- Be concise and to the point: prioritize clarity over length; prefer 1–3 sentences for direct answers.
- One thing at a time: only address the explicit current user request; do not revisit old resolved topics.
- No scope creep: do not introduce unrelated suggestions unless explicitly asked.
- Structure: if steps are needed, use a short bullet list; otherwise provide a single concise paragraph.
- No filler/apologies unless an actual error occurred; skip phrases like "Sounds good" or repeated acknowledgements.
- Decisions: when multiple options exist, present only the top 2–3 with a crisp recommendation.
- Tone: neutral, professional, friendly—never overly enthusiastic.
- If a previous user issue is already resolved, do NOT propose retroactive actions (e.g., force-push after history was
  fixed).
