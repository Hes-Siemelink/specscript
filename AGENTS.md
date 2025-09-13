# AGENTS.md

This file provides guidance to AI assistants (Claude Code, GitHub Copilot, etc.) when working with code in this repository.

## Project Overview

SpecScript is a Kotlin-based tool for creating human and AI-friendly specifications using Markdown and YAML. It provides a CLI tool called `cli` that can execute `.cli` script files containing YAML specifications for HTTP requests, user interaction, testing, and more.

## Build and Development Commands

### Build the project
```bash
./gradlew build
```

### Run tests
```bash
./gradlew test                    # Unit tests
./gradlew specificationTest       # Specification tests (392 tests)
./gradlew check                   # All tests including specification tests
```

### Create executable
```bash
./gradlew build fatJar
# Thin JAR (requires classpath)
alias cli-thin="java -jar `pwd`/build/libs/specscript-*.jar"
# Fat JAR (self-contained)
alias cli="java -jar `pwd`/build/libs/specscript-*-full.jar"
```

### Run examples
```bash
cli samples/hello.cli
cli samples                       # Interactive selection
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
- **Commands**: `src/main/kotlin/specscript/commands/` - Implementation of all script commands (HTTP, testing, control flow, etc.)
  - Commands use paths like `"core/testing"` to reference specification files
  - 56+ command implementations organized by functionality
- **File Handling**: `src/main/kotlin/specscript/files/` - .cli file parsing and management
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
- Uses Javalin for HTTP handling
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
- Example test pattern:
```yaml
Test case: MCP operation
  setup:
    Start mcp server:
      name: test-server
      tools: { ... }
  test:
    # Test operations
  cleanup:
    Stop mcp server:
      name: test-server
```

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
  - All 392 specification tests run executable documentation
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
- **Ktor** for HTTP client functionality
- **Javalin** for HTTP server capabilities
- **JUnit 5 + Kotest** for testing
- **Gradle** with Kotlin DSL for build management

### Coding Conventions

- **JSON Schema:** For map-like structures, use `additionalProperties: { ... }`. Do not use `patternProperties`.
- **Kotlin:** Follow the standard Kotlin style guide.
- **Good OO / encapsulation:** Encapsulate logic within appropriate classes and provide utility functions to reduce client code complexity. For example, McpServer encapsulates server context management rather than exposing raw session key manipulation.
- **File Naming:** Specification files use spaces in their names (e.g., `Mcp server.spec.md`), which is important for
  file system operations.

## Important Notes

- The project uses "specscript" internally but is branded as "SpecScript" externally
- All specifications are executable - documentation doubles as test cases
- The `specification/` directory is included as a resource directory for runtime access
- The CLI supports interactive mode with prompts and non-interactive mode with command-line arguments
- Command implementations reference `"core/"` paths instead of legacy `"specscript/"` paths
- Two JAR artifacts are built: thin (531KB) and fat (36MB) for different deployment scenarios
- Run the tests before creating a commit

## Strategic Planning and Development Approach

### Spec-First Development Philosophy
SpecScript follows a **spec-first development methodology**:
1. **Always write specifications first** - Define behavior in SpecScript YAML/Markdown format
2. **Analyze existing patterns** - Study current Kotlin implementation before proposing solutions
3. **Create detailed plans** - Document approach, phases, and integration considerations in `/plan` directory
4. **Implement incrementally** - Build based on specification, validating against existing patterns

### Planning Documents Structure
- **Focused Plans**: Single-responsibility plans like `mcp-backlog-focused-plan.md`
- **Future Work Plans**: Separate plans for dependent work like `server-alignment-future-plan.md`
- **Learning Integration**: Document insights and lessons learned for future reference
- **Phase-based Implementation**: Clear phases with dependencies and success criteria

### Key Planning Principles
- **Scope Discipline**: Separate immediate needs from future architectural work
- **Learning-Based**: Build on insights from current implementations
- **Backward Compatibility**: Maintain existing functionality while adding new capabilities
- **Risk Mitigation**: Identify technical and implementation risks with mitigation strategies

## Major Refactoring Plans

- **Library Architecture Refactoring**: See `plan/specscript-to-specscript-library-refactoring.md`
- **MCP Backlog System**: See `plan/mcp-backlog-focused-plan.md` (immediate focus)
- **Server Alignment**: See `plan/server-alignment-future-plan.md` (future work)
- Goal: Transform SpecScript into a library that specscript depends on
- Will enable separation of core engine from CLI-specific implementations
- Planned multi-repository development setup for easier cross-repo work

## Refactoring Rules & Guidelines

- When refactoring, follow a multi-phase approach, ensuring each phase is completed and tested before proceeding.
- During refactoring, the AI assistant should read the log file (`.claude/log.txt`) to understand the current state and continue from where it left off.
- When providing options during refactoring, clearly state the options and the decision made. In the case of dependency management, prefer using thin JARs and let Gradle handle transitive dependencies (Option 1).
- After removing implementation directories during refactoring, update the build configuration to depend on the new library, create a minimal CLI bootstrap, and test the setup.
- When working in a git-controlled directory (e.g., a dedicated branch for refactoring), avoid creating unnecessary backup files, as git serves as the backup system.
- When refactoring and there is a naming conflict (e.g., main functions in different projects), rename one of the conflicting elements to resolve the conflict.

## Git commit rules

When committing changes to the project, follow these rules:

- Use: `git commit -m "Summary" -m "content"`
- Summary:
    - Must not exceed 70 characters.
    - Write the summary as a user-focused release note item, describing the functional change or improvement from the
      perspective of a non-developer stakeholder.
    - Avoid technical or code-centric language in the summary; save those details for the content/body.
    - If a new feature is added, start the summary with the 💫 emoji.
    - For non-functional updates (refactoring, documentation, bug fixes, etc.), do not use any emoji.
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

## SpecScript Documentation and Testing Philosophy

**Key Learning**: SpecScript documentation IS the test suite. Every code example in specification documents literally executes during tests.

**CRITICAL**: The files in `specification/language/` are the authoritative reference for writing effective SpecScript. These documents define the canonical patterns and must be consulted before writing any specification documents. Key references:
- `SpecScript Yaml Scripts.spec.md` - Core language syntax and command usage
- `SpecScript Markdown Documents.spec.md` - Defines proper structure for spec files including hidden cleanup code
- `Organizing SpecScript files in directories.spec.md` - File organization patterns
- Other language specifications provide the authoritative guidance for SpecScript syntax and structure

### Code Block Types in SpecScript Docs:
- ````yaml specscript` - **EXECUTABLE**: Runs as actual tests during the gradle specificationTest
- ````yaml` - **ILLUSTRATIVE**: Shows syntax without execution (for invalid/example code)
- ````yaml file=filename.cli` - **FILE CREATION**: Creates temporary files during test execution

### Critical Rules for SpecScript Documentation:
1. **All `yaml specscript` blocks must be valid, working code** - they execute during tests
2. **Use `localhost:2525` endpoints** - sample-server.cli runs automatically during tests providing real HTTP endpoints
3. **Invalid examples must use plain `yaml`** - never `yaml specscript` for broken/incorrect code
4. **You cannot lie in documentation** - if you write it, it must work or tests fail
5. **Docs stay current automatically** - because they're executable, they can't become outdated
6. **Use sentence case for section titles** - e.g., "Reading temp files created in Markdown", not "Reading Temp Files Created In Markdown"
7. **Multiple commands require `---` separators** - YAML doesn't allow duplicate keys, so use `---` between commands or list syntax for repetition

### Sample Server Endpoints (available during tests):
- `POST /greeting` - accepts `name` and `language`, returns formatted greeting
- `GET /items` - returns `[1, 2, 3]`  
- `POST /items` - echoes back input as `Fields: ${input}`
- `GET /hello` - returns `"Hello from SpecScript!"`

### Documentation Development Pattern:
1. Write specification with executable examples
2. Run `./gradlew specificationTest`
3. If tests fail, your documentation is wrong - fix it
4. This is **spec-driven development** - documentation validates behavior

**This is the magic of SpecScript**: Documentation that can't lie because it executes.

## Adding New SpecScript Commands: Complete Development Process

This section documents the complete process used to create the `Mcp tool` command, serving as a template for future command development.

### Phase 1: User-Centered Specification Writing
1. **Write basic use case**: Start with the simplest, most natural way a user would want to use the command
2. **Create declarative examples**: Focus on *what* the user wants to achieve, not *how* it works internally
3. **Build specification document**: Write `.spec.md` with realistic examples that make intuitive sense
4. **Expand use cases**: Add more complex scenarios, edge cases, and variations based on natural usage patterns
5. **Validate readability**: Ensure the specification reads clearly and demonstrates obvious value to users

### Phase 2: Technical Design
1. **Study existing patterns**: Examine similar commands (e.g., `Mcp server`) for structural consistency
2. **Design YAML structure**: Determine content type support (Value/List/Object) based on specification examples
3. **Create JSON schema**: Define `schema/CommandName.schema.yaml` following established patterns
4. **Refine specification structure**: Apply `SpecScript Markdown Documents.spec.md` patterns for proper execution

### Phase 3: Kotlin Implementation
1. **Create command handler**: Implement `CommandHandler` extending appropriate interfaces (`ObjectHandler`, `DelayedResolver`)
2. **Design data classes**: Reuse existing classes where possible (good OO/encapsulation principle)
3. **Implement business logic**: Focus on core functionality, delegate to existing utility methods
4. **Add to CommandLibrary**: Register the new command in the centralized command registry
5. **Handle shared concerns**: Utilize existing patterns for context management, server registries, etc.

### Phase 4: Integration and Testing
1. **Build and test**: Use `./gradlew build fatJar -x test -x specificationTest` for rapid iteration
2. **Validate specification**: Run individual spec files to verify examples work correctly
3. **Full test suite**: Execute `./gradlew build` to ensure all tests pass
4. **Refactor based on feedback**: Apply architectural improvements (encapsulation, code reuse)

### Key Architectural Decisions Made
- **Data structure reuse**: Modified existing `ToolInfo` class rather than creating duplicates
- **Context management**: Added utility functions to `McpServer` for session state encapsulation
- **YAML structure evolution**: Evolved from individual tool definitions to map-based structure for consistency
- **Proper spec structure**: Separate YAML blocks, descriptive text flow, hidden cleanup code

### Critical Patterns for Future Commands
- **User-first specification**: Start with how users naturally want to express their intent, not technical constraints
- **Declarative examples**: Focus on *what* users want to achieve, making the command's purpose immediately clear
- **Specification-driven design**: The readability and intuitiveness of the specification drives all technical decisions
- **Good OO/encapsulation**: Create utility functions in parent classes to reduce client code complexity
- **Structural consistency**: Follow existing command patterns only after the user experience is well-defined
- **Test-driven validation**: Every code example in specifications must execute successfully
- **Context sharing**: Use `ScriptContext.session` for cross-command state when appropriate

### Why Specification-First Matters
Implementation-first approaches often result in:
- Awkward, technical-sounding command structures
- Documentation that's hard to write because the design is unintuitive
- Commands that feel like programming rather than declaring intent
- Complex examples that obscure the command's value

Specification-first ensures:
- Natural, declarative command usage that reads like human intent
- Documentation that flows logically and demonstrates clear value
- Implementation that serves user needs rather than technical convenience
- Commands that feel like configuration rather than code

This process ensures new commands integrate seamlessly with the existing SpecScript ecosystem while maintaining consistency and testability.

## Spec-First Development Philosophy

SpecScript enables true **specification-driven development** where the specification IS the implementation:

### Core Principles
1. **Write the spec first** - Define how the system should work in `.spec.md` files with executable examples
2. **Implement in SpecScript** - Use pure SpecScript (YAML) before writing any Kotlin/Java code
3. **Mock with `Output` commands** - Use `Output:` to return mock data that demonstrates the intended behavior
4. **Iterate on the specification** - Test and refine the spec until it perfectly describes the desired system
5. **Only then implement in code** - Convert SpecScript implementations to optimized code when needed

### Development Workflow
1. **Create specification**: Write `.spec.md` with complete documentation and examples
2. **Implement in SpecScript**: Create `.cli` files with mock implementations using `Output` commands
3. **Test the specification**: Run `cli your-spec.spec.md` to validate syntax and behavior
4. **Iterate and refine**: Adjust the specification based on testing and feedback
5. **Replace mocks gradually**: Convert mock `Output` commands to real implementations when needed

### Benefits
- **Immediate feedback**: Specifications are executable from day one
- **Clear requirements**: The spec defines exactly how the system should behave
- **No implementation-spec drift**: The spec IS a working implementation
- **Faster iteration**: Changes to behavior start with changing the spec
- **Better collaboration**: Non-technical stakeholders can understand and run the specs

### Example: MCP Server Development
```markdown
1. Write `mcp-server.spec.md` with complete MCP tool definitions using `yaml specscript` blocks
2. Implement tools with `script: Output: { mock: "data" }` 
3. Test with `cli mcp-server.spec.md`
4. Iterate on tool schemas and responses
5. Replace `Output` with real logic when spec is solid
```

### Critical File Extension Rules
- **`.spec.md`**: Executable SpecScript documentation with ````yaml specscript` blocks
- **`.cli`**: Pure SpecScript YAML files
- **NEVER use**: ````yaml file=filename.cli` in executable documentation - this creates temp files, not executable code

### Common Mistakes to Avoid
1. **Wrong Block Type**: Using ````yaml file=` instead of ````yaml specscript` in `.spec.md` files
2. **Eager Commits**: Always run `./gradlew specificationTest` before committing - we are spec-driven!
3. **Wrong Commit Messages**: 
   - Use "Bug fix" for actual bugs
   - Use functional change descriptions for user-facing changes
   - Use change emoji (💫) for new features
   - Use warning triangle (⚠️) for breaking changes
   - Do not mention test results unless tests are failing
   - Example: "💫⚠️ Changed Markdown extension from 'cli.md' to 'spec.md'"

## Documentation Style Guidelines

### Markdown Headers
- **Never use emojis in Markdown headers** (e.g., avoid `## 🔄 Features`)
- Keep headers clean and professional with text only
- Emojis can be used in content, but not in section titles

# Agent tone

- I am a grumpy old European with 20 years of experience in software engineering.
- Be concise and to the point.