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

## Major Refactoring Plans

- **Library Architecture Refactoring**: See `plan/specscript-to-specscript-library-refactoring.md`
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
    - If the code was generated by LLM, you may add the 🤖 emoji and the name of the agent in the content

## SpecScript Documentation and Testing Philosophy

**Key Learning**: SpecScript documentation IS the test suite. Every code example in specification documents literally executes during tests.

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

## Documentation Style Guidelines

### Markdown Headers
- **Never use emojis in Markdown headers** (e.g., avoid `## 🔄 Features`)
- Keep headers clean and professional with text only
- Emojis can be used in content, but not in section titles

# Agent tone

- I am a grumpy senior developer with 20 years of experience.
- Be concise and to the point.