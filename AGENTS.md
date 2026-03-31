# AGENTS.md

We're in the SpecScript language repo and the entire language is defined in the `specification` dir. Read the
specification to know what SpecScript is. Starting points: Main README.md, `specification/overview` directory, and
`specification/language/` directory.

**IMPORTANT**: before you do anything else, run the `beans prime` command and heed its output. `beans prime` configures
the issue tracker for the session.

Use `.tmp` in this repo as a temp directory so you don't need to ask for permission to write files somewhere else.

## Project Overview

SpecScript is a tool for creating human and AI-friendly specifications using Markdown and YAML. It provides a CLI tool
called `spec` that can execute `.spec.yaml` script files containing YAML specifications for HTTP requests, user
interaction, testing, and more.

The `specification/` directory contains the complete language specification written in SpecScript Markdown itself. This
includes tests written in SpecScript yaml. The `samples` directory contains SpecScript example code. The `src/`
directory contains the Kotlin implementation (reference implementation). The `typescript/` directory contains a
TypeScript implementation. Both implementations pass the same specification tests.

When doing a full build, both the specification tests and the unit tests run. The specification tests execute all the
code examples in the documentation, ensuring that the documentation is always accurate and up-to-date.

## Development Process

SpecScript is developed using a spec-first approach, a bit like TDD. Write the specification BEFORE implementing code.
The spec defines the behavior and serves as documentation and tests; implementation follows.

Full steps are:

1. User gives a problem statement or feature request.
2. Analyze the problem and write a proposal. Store the proposal as a plain Markdown file in the `plan/proposals`
   directory. The Markdown is not specscript yet, just a regular Markdown document describing the problem, the proposed
   solution, and any relevant details. Depending on the problem statement, this could be high level (new language
   feature), mid-level (new command), or low level (bug fix or refactoring).
3. User reviews and confirms the proposal. Do not proceed without explicit confirmation.
4. Write the spec. This is the most critical step. The spec defines the behavior and serves as documentation and tests.
   Write it in the `specification/` directory. For invasive changes, put the new or heavily revised spec in
   `plan/draft-specs`.
5. User reviews and confirms the spec. Do not proceed without explicit confirmation.
6. Implement the code in Kotlin. IMPORTANT: follow existing patterns and architecture. Do not introduce new patterns or
   architectural styles without explicit confirmation. Put any suggestions for improvements in `plan/agent-ideas.md` as
   concise one-liners to be reviewed later.
7. User reviews and confirms the implementation. Do not proceed without explicit confirmation.
8. Prepare commit according to Git commit rules below. User will review and push.

Tips for spec-first iteration:

- Optionally mock with `Output:` commands for early iteration before implementing
- Run `./gradlew specificationTest` — tests fail means the spec or implementation is wrong
- To change behavior, change the spec first, then update the implementation

Plans live in the `/plan` directory. When refactoring, follow a multi-phase approach — complete and test each phase
before proceeding. Git is the backup system — don't create backup files in a git-controlled directory.

## Build and Development Commands

```bash
./gradlew build                   # Full build
./gradlew test                    # Unit tests
./gradlew specificationTest       # Specification tests (440+ tests)
./gradlew check                   # All tests including specification tests
./gradlew build fullJar           # Build fat JAR (self-contained)
./gradlew githubRelease           # Create GitHub release (pass -PreleaseHeadline="..." for description)
```

Create a local executable:

```bash
alias spec="java -jar `pwd`/build/libs/specscript-*-full.jar"
spec specification/hello-world.spec.yaml   # Run a script
spec samples                                 # Interactive selection
```

## Architecture

### Core Components

- **CLI Entry Point**: `src/main/kotlin/specscript/cli/Main.kt`
- **Language Engine**: `src/main/kotlin/specscript/language/` — core language processing and command execution
- **Command Registry**: `src/main/kotlin/specscript/language/CommandLibrary.kt` — where commands are registered
- **Commands**: `src/main/kotlin/specscript/commands/` — 56+ command implementations organized by functionality
    - Commands use paths like `"core/testing"` to reference specification files
- **Command Schemas**: `src/main/kotlin/specscript/schema/` — JSON Schema definitions for command YAML structure
- **File Handling**: `src/main/kotlin/specscript/files/` — .spec.yaml file parsing and management
- **Utilities**: `src/main/kotlin/specscript/util/` — JSON/YAML processing, I/O utilities
- **Unit Tests**: `src/tests/unit/`
- **Specification Tests**: `src/tests/specification/` — runs all executable documentation (440+ tests)

### Specification Directory

The `specification/` directory is the complete language specification written in executable Markdown:

- `specification/language/` — language syntax and features
- `specification/commands/core/` — core command reference with examples
- `specification/commands/ai/` — AI-related commands (MCP server, etc.)
- `specification/cli/` — CLI tool usage

All documentation includes runnable code examples executed as part of the test suite. The `specification/` directory is
included as a resource directory for runtime access.

### Architectural Principles

- The project is split into `specification` (the language specification) and `src` (the Kotlin implementation).
- The `typescript/` directory contains an independent TypeScript implementation that passes the same specification
  tests.
- Commands are implemented as singleton objects extending `specscript.language.CommandHandler`.
- The command's name and group are registered in the `CommandHandler` constructor, e.g.,
  `CommandHandler("Mcp server", "ai/mcp")`.
- The command logic is implemented in the `execute` method.
- Two JAR artifacts are built: thin and fat for different deployment scenarios.
- The project uses "specscript" internally but is branded as "SpecScript" externally.
- Command implementations reference `"core/"` paths instead of legacy `"specscript/"` paths.

### Porting guides

Lessons from the TypeScript port are captured in `plan/proposals/`:

- `language-designer-lessons-learned.md` — spec gaps, surprising behaviors, and improvement suggestions discovered
  during the TypeScript port
- `go-implementer-guide.md` — level-by-level implementation guide with gotchas, for future implementations

## Key Technologies

- **Kotlin 2.1.20** with JVM target 21
- **Jackson** for JSON/YAML processing
- **Ktor** for HTTP client and server functionality
- **JUnit 5 + Kotest** for testing
- **Gradle** with Kotlin DSL for build management

## Coding Conventions

- **JSON Schema:** For map-like structures, use `additionalProperties: { ... }`. Do not use `patternProperties`.
- **Kotlin:** Follow the standard Kotlin style guide.
- **Declarative style:** More "what" than "how". Code should speak for itself. Prefer a purpose-named method that
  describes *what* you want over exposing mechanism at the call site. Null handling, type parameters, and
  library-specific ceremony belong behind the API. For example, prefer `Json.toObject(nullableMap)` over
  `Json.valueToTree<ObjectNode>(nullableMap ?: emptyMap<String, Any>())`.
- **Good OO / encapsulation:** Encapsulate logic within appropriate classes and provide utility functions to reduce
  client code complexity. For example, McpServer encapsulates server context management rather than exposing raw session
  key manipulation.
- **Good naming over comments:** If you need a comment, the name is wrong. Use `typealias` to make type signatures
  self-documenting. No abbreviations (e.g., don't use "FQN" — use "full name").
- **Sparse commenting:** Most code should be self-explanatory. Use comments only for TODOs, non-obvious business logic,
  and KDoc for public methods when purpose isn't immediately clear. Delete comments that describe what the code already
  says.
- **Formatting:** Blank line before `else` in `when` blocks.
- **File Naming:** Specification files use spaces in their names (e.g., `Mcp server.spec.md`), which is important for
  file system operations.
- **Directory config:** Each directory can have a `specscript-config.yaml` file for metadata (description, imports,
  connections). Loaded by `DirectoryInfo.kt`. Not a SpecScript script — plain YAML config.

## Specification writing style

Spec documents are written spec-first — they define behavior before implementation exists. This drives a minimalist
style: precise, executable, and cheap to change.

- **One executable example per concept**, two maximum per section. The example IS the test.
- **No motivational prose.** No "this is useful for", "previously you had to", or "in CI/CD environments" paragraphs.
- **No illustrative non-executable examples.** If it can't run as a test, it probably doesn't belong in the spec.
- **Don't explain what the reader can infer** from the example or from a stated resolution order.
- **Prefer a summary table** over multiple explanatory paragraphs.
- **Add new features as sections in existing spec files** rather than creating new spec files.
- **Keep prose to one or two sentences** introducing the feature and its core rule. Then show the example.
- **Edge cases go in `tests/` files**, not in the main spec document.
- **Always introduce code blocks with text.** Markdown viewers hide the ```` ``` ```` header, so the reader cannot see
  block type, language, or `file=` annotations. Every code block needs a preceding sentence that provides context — what
  the block is, what file it creates, or what it demonstrates.

This style is deliberately dry. Friendlier tutorial-style guides are a separate concern (see TODO.md).

## Specification document hygiene

Lessons from implementing a second (TypeScript) implementation against the spec:

- **Avoid asserting on exact formatted output** when the intent is to test behavior, not formatting. Use pattern
  matching or partial assertions. Exact output assertions make formatting quirks de-facto spec — a second implementation
  is forced to replicate bugs.
- **YAML output format varies between libraries** (indentation, quoting style, flow vs. block). Prefer comparing parsed
  structures (semantic equality) over formatted YAML strings where possible.
- **Watch for cross-level contamination** in spec files. If a Level 0–1 spec file uses `file=` blocks or `shell cli`
  blocks (Level 3+ features), it forces test runners to handle partial failures. Keep spec sections within their level,
  or move cross-level sections to separate files.

## Sensitive areas

- **`specification/code-examples/basic/` directory**: Adding or removing files here breaks 2 spec files that hardcode
  directory listing output (`Running SpecScript files.spec.md`, `Command line options.spec.md`). Update those specs if
  you modify the directory contents.

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
    - For new commands specifically, use `💫 New command: Command name` or `💫 New commands: Name1, Name2` as the summary.
    - If the change is a breaking change, use `💫 ⚠️` (both emojis) at the start of the summary. This signals a MINOR
      version bump is needed at release time. For breaking new commands: `💫 ⚠️ New commands: Name1, Name2`.
    - For non-functional updates (refactoring, documentation, etc.), do not use any emoji.
    - For bug fixes, use exactly: `Bug fix` as the summary.
    - For invasive code structure changes without functional updates, use exactly: `Refactoring` as the summary.
    - For documentation updates, use exactly: `Documentation` as the summary.
    - For code cleanups (removing unused code, imports, fixing linter warnings, etc. without invasive refactoring), use
      exactly: `Code Cleanup` as the summary.
    - For updates to AI agent context, conversation history, distilled rules, or project meta files (such as .specstory,
      .cursor, AGENTS.md or similar), use exactly: `AI Context` as the summary. Do not mix these with other changes.
    - For (leftover) planning files in plan or .beans directory, commit them together with exactly: `Plan` as the
      summary. No body content needed.
- Content:
    - Each bullet or paragraph must be a separate `-m` argument.
    - Do not use `\n` to separate topics within a single `-m` argument.
    - Do not use backticks (`) in commit messages.
    - Do not mention test status unless there are failing tests requiring attention
    - If the code was generated by LLM, you may add the 🤖 emoji and the name of the agent in the content

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
