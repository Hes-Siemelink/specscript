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
./gradlew specificationTest       # Specification tests
./gradlew integrationTest        # Integration tests
./gradlew check                   # All tests including specification tests
```

### Create executable
```bash
./gradlew build
alias cli="java -jar `pwd`/build/libs/specscript-*.jar"
```

### Run examples
```bash
cli samples/hello.cli
cli samples                       # Interactive selection
```

### Release process
```bash
./gradlew githubRelease          # Create GitHub release
```

## Architecture

### Core Components

- **CLI Entry Point**: `src/main/kotlin/instacli/cli/Main.kt` - Main application entry point
- **Language Engine**: `src/main/kotlin/instacli/language/` - Core language processing and command execution
- **Commands**: `src/main/kotlin/instacli/commands/` - Implementation of all script commands (HTTP, testing, control flow, etc.)
- **File Handling**: `src/main/kotlin/instacli/files/` - .cli file parsing and management
- **Utilities**: `src/main/kotlin/instacli/util/` - JSON/YAML processing, I/O utilities

### Test Structure

- **Unit Tests**: `src/tests/unit/` - Traditional unit tests
- **Specification Tests**: `src/tests/specification/` - Tests that validate the SpecScript language specifications
- **Integration Tests**: `src/tests/integration/` - End-to-end integration tests

### Documentation and Specifications

The `instacli-spec/` directory contains the complete language specification written in executable Markdown:
- `instacli-spec/language/` - Language syntax and features
- `instacli-spec/commands/` - Command reference with examples
- `instacli-spec/cli/` - CLI tool usage

All documentation includes runnable code examples that are executed as part of the test suite.

## Key Technologies

- **Kotlin 2.1.20** with JVM target 21
- **Jackson** for JSON/YAML processing
- **Ktor** for HTTP client functionality
- **Javalin** for HTTP server capabilities
- **JUnit 5 + Kotest** for testing
- **Gradle** with Kotlin DSL for build management

## Important Notes

- The project uses "Instacli" internally but is branded as "SpecScript" externally
- All specifications are executable - documentation doubles as test cases
- The `instacli-spec/` directory is included as a resource directory for runtime access
- The CLI supports interactive mode with prompts and non-interactive mode with command-line arguments