# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

## Important Notes

- The project uses "specscript" internally but is branded as "SpecScript" externally
- All specifications are executable - documentation doubles as test cases
- The `specification/` directory is included as a resource directory for runtime access
- The CLI supports interactive mode with prompts and non-interactive mode with command-line arguments
- Command implementations reference `"core/"` paths instead of legacy `"specscript/"` paths
- Two JAR artifacts are built: thin (531KB) and fat (36MB) for different deployment scenarios

## Major Refactoring Plans

- **Library Architecture Refactoring**: See `plan/specscript-to-specscript-library-refactoring.md`
- Goal: Transform SpecScript into a library that specscript depends on
- Will enable separation of core engine from CLI-specific implementations
- Planned multi-repository development setup for easier cross-repo work
- Run the tests before creating a commit