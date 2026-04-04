# SpecScript Go Implementation Guide

Lessons from porting SpecScript from Kotlin to TypeScript, distilled for a Go implementer.

## High-Level Plan

Use the **language levels** system (`specification/levels.yaml`). It defines 7 levels (0–6) ordered by bootstrap
dependency, not importance. Each level adds commands and language features. An implementation at level N must pass all
spec tests for levels 0 through N.

### Recommended implementation order

| Level | Name | Commands | Estimated effort |
|-------|------|----------|-----------------|
| 0 | Core Runtime | 21 | 2–3 days |
| 1 | Control Flow and Data | 22 | 2–3 days |
| 2 | Markdown Documents | 0 (scanner/converter) | 0.5 day |
| 3 | Files, Shell, Script Composition | 6 | 1–2 days |
| 4 | HTTP | 9 | 1–2 days |
| 5 | User Interaction and Connections | 12 | 1–2 days |
| 6 | SQLite / MCP | 9 (independent modules) | 1–2 days each |

Levels 3 and 4 are independent — implement in either order. Level 6 modules are also independent of each other.

### Testing strategy (three phases)

1. **Levels 0–1**: Write an external test harness that reads `.spec.yaml` files and runs them. The `Test case`, `Assert
   equals`, `Assert that`, `Expected output`, `Expected console output`, and `Expected error` commands are all Level 0,
   so you can self-test from day one.
2. **Level 2**: Implement the Markdown scanner/converter. This unlocks running `.spec.md` files as tests — the
   implementation can now test itself against the specification documents.
3. **Levels 3+**: Full conformance testing. Every spec file in `specification/` should be runnable.

## Architecture Mapping: Kotlin → Go

### Core data model

Kotlin uses Jackson's `JsonNode` tree (`ObjectNode`, `ArrayNode`, `ValueNode`, `MissingNode`). TypeScript used native
JS objects. For Go:

- **Use `map[string]any` for objects, `[]any` for arrays, and primitive types directly.** This is the Go-idiomatic
  equivalent of "JSON as native data."
- You will need helper functions for: deep-copy, deep-merge, type checking, path navigation (`a.b[0].c`), and
  comparison.
- **`MissingNode` equivalent**: Kotlin chains `MissingNode` for unresolved paths (`${obj.missing.nested}` returns empty
  string). TypeScript throws. The spec doesn't explicitly define the behavior, but Kotlin's behavior is the reference.
  Consider using a sentinel value or returning `nil` with a "missing" flag.

### Command handler interface

```go
type CommandHandler interface {
    Name() string
    Group() string  // e.g., "core/testing"
    Execute(data any, ctx *Context) (any, error)
    
    // Flags
    HandlesLists() bool      // If true, arrays are passed directly. If false, runtime auto-iterates.
    DelayedResolver() bool   // If true, skip resolve() before calling Execute.
    ErrorHandler() bool      // If true, errors from previous commands are available in context.
}
```

Register all commands in a `map[string]CommandHandler`. The command name is the YAML key (case-insensitive matching
recommended, Kotlin lowercases).

### The resolve pipeline (CRITICAL — read carefully)

This is the most under-documented and most important part of the language. The pipeline runs on every command's data
before `Execute` is called (unless `DelayedResolver` returns true):

1. **Variable resolution**: Replace `${varName}` in strings. If the entire string is `${varName}`, replace with the
   actual typed value (not string). Walk objects and arrays recursively, but **only resolve values, never keys**.
2. **Eval expressions**: Replace `/CommandName expression` inline syntax. This calls `runCommand()` recursively.
3. **Conditions**: Expand `is`, `is not`, `contains`, `matches`, comparison operators used in `If`, `When`, etc.

**Gotchas**:
- Object keys must NOT be resolved. `${index}: 1` is a pattern where the key is a variable reference that the *command*
  interprets, not a variable to substitute. Resolving keys breaks `Add to` and similar commands.
- Eval runs during resolve, before the command sees the data. This means eval results are available as values in the
  command's input.
- `DelayedResolver` commands must call `resolve()` explicitly on sub-portions of their data. They opt out of the
  automatic pass because they need to control when resolution happens (e.g., `If` resolves conditions before branches,
  `For each` resolves the body per iteration).

### Auto-list iteration

When a command receives an array and `HandlesLists()` returns false, the runtime iterates the array and calls `Execute`
once per element, collecting results into a new array.

**This is the #1 trap for new implementations.** Every command must explicitly declare its list handling. There is no
safe default. Test suites don't catch wrong declarations because few spec tests exercise commands with array input.

Commands that handle lists themselves (non-exhaustive): `Do`, `For each`, `Print`, `Assert equals`, `Tests`, `Run
script`, `Json patch`, `Http server`, `Http endpoint`.

### Context

```go
type Context struct {
    Variables  map[string]any  // Current scope variables
    Output     any             // Last command output
    Session    map[string]any  // Shared across script calls (answers, server instances, etc.)
    ScriptDir  string          // Directory containing the current script
    WorkingDir string          // Current working directory
    TempDir    string          // Temporary directory for this execution
    Error      error           // Last error (for error handlers)
    
    // Stdout capture
    LogCallback func(string)   // How Print sends output
}
```

Child contexts (for `Run script`, `Do`, etc.) share `Session` by reference but get fresh `Variables`.

## Level-by-Level Gotchas

### Level 0: Core Runtime

- **YAML multi-document parsing**: A `.spec.yaml` file may contain multiple `---`-separated documents. Each document is
  one command. Also support top-level arrays: `[{Print: hello}, {Print: world}]`.
- **YAML duplicate key detection**: Standard YAML parsers do last-wins on duplicate keys. SpecScript needs first-wins or
  error detection. Both Kotlin and TypeScript walk the YAML AST directly.
- **`Expected error`**: The value is a YAML map with a `type` key (matched against the error type/class name) and an
  optional `message` key (substring match). If the value is a plain string, it's the custom error message for
  `MissingExpectedError` (the error that fires when no error occurred).
- **`Answers` command**: Stores pre-defined answers in `Session` for use by `Prompt`, `Input parameters`, etc. Essential
  for non-interactive testing. Not just a testing convenience — it's the mechanism that makes spec tests work.

### Level 1: Control Flow and Data

- **`Add` is the most polymorphic function**: object+object merge, object+array, array+array concatenation, array+scalar
  append, scalar+scalar, string+string concatenation. ~6 type combinations.
- **`For each` loop variable leaks** into the parent scope after the loop completes. This matches Kotlin behavior and is
  likely a bug, but changing it would break scripts.
- **`Repeat` has no termination guard**: An incorrect `until` condition creates an infinite loop. Consider adding a max
  iteration limit.
- **Error type coercion**: Error `type` values in YAML may be parsed as integers (e.g., `400`). Coerce to string before
  matching.
- **`Wait` must block synchronously**: In Go, `time.Sleep()` works perfectly — no workarounds needed (unlike Node.js
  which required `Atomics.wait`).

### Level 2: Markdown Documents

- **Scanner is a line-by-line state machine** that classifies code blocks by type. Block type detection must use an
  ordered list (first match wins), not a map.
- **Priority order matters**: `yaml file` before `yaml specscript`, `shell cli` before `shell`.
- **Empty string edge case**: If your scanner tracks `lastLinePrefix`, be careful with empty-string sentinel values.
  The TypeScript port hit a bug where `""` (empty prefix for headers) was falsy and broke end-of-block detection.
- **Files with no executable blocks** (e.g., `Best Practices.spec.md`) should produce an empty test suite, not an error.

### Level 3: Files, Shell, Script Composition

- **YAML block scalar trailing newlines**: Different YAML libraries handle trailing newlines differently on block
  scalars. Test carefully with `Temp file` → `Read file` → `Expected output` chains.
- **Three directory concepts**: `scriptDir` (where the script file lives), `workingDir` (CWD for shell commands and
  relative file paths), `tempDir` (per-execution temp directory). Plus `SCRIPT_HOME` env var (points to the SpecScript
  installation for resource lookup). Getting these wrong causes subtle failures.
- **Local file commands**: A file `create-greeting.spec.yaml` in the script's directory becomes command `Create
  greeting`. Resolution order: registered commands → imports → local files.
- **`Shell` command env vars**: Environment variables are serialized as YAML strings, not JSON. Downstream scripts parse
  them.
- **Markdown `shell` blocks default `show output: true`**; YAML `Shell:` command defaults to `false`. Different
  defaults for the same command depending on context.

### Level 4: HTTP

- **Go advantage**: goroutines + channels make the HTTP server trivial. No need for the three-process architecture that
  TypeScript required or the coroutine management that Kotlin uses. Start the server in a goroutine, use a channel for
  the ready signal.
- **`Http server` is a DelayedResolver**: Endpoint handler content is resolved at request time, not server start time.
  The handler body is a template, not pre-resolved code.
- **Response body parsing**: Parse as YAML first (which subsumes JSON), fall back to raw string.
- **Header case**: Preserve original HTTP header casing. Go's `net/http` canonicalizes headers
  (`content-type` → `Content-Type`), but some spec tests check exact original casing. Use `req.Header` carefully.
- **Hardcoded test ports**: Tests use ports 2525, 25001–25012. Potential for conflicts.
- **Request parameter merging**: Scalars override, nested objects deep-merge, body overrides entirely, path appends.

### Level 5: User Interaction

- **`Prompt` dispatch**: Check the answers map (from `Answers` command in session) first. If found, simulate the prompt
  with test output format. If not found, show real interactive prompt.
- **`Prompt object` is a DelayedResolver**: Earlier field answers become variables for later field conditions.
- **Test output format must be exact**: `? message answer` and `❯ ◉ ◯` characters for choice rendering.

## Go-Specific Recommendations

### What maps cleanly

- **Command dispatch**: `map[string]CommandHandler` — trivial in Go.
- **JSON data model**: `map[string]any` / `[]any` — natural fit, no wrapper classes needed.
- **HTTP server**: goroutines make the concurrent server+client pattern trivial.
- **Synchronous execution**: Go is natively synchronous with goroutines for concurrency — perfect match for
  SpecScript's execution model (unlike Node.js).
- **Shell commands**: `os/exec` — straightforward.
- **File I/O**: Standard library covers everything.

### What needs careful design

- **Variable resolution with type preservation**: When `${var}` is the entire string, replace with the typed value. When
  it's embedded in a larger string, interpolate as string. This requires walking the data tree and distinguishing
  "whole-value replacement" from "string interpolation."
- **YAML parsing**: Use `gopkg.in/yaml.v3` — it supports multi-document parsing, AST access for duplicate key
  detection, and block scalar handling. Test trailing newline behavior early.
- **Error types**: Go doesn't have exception hierarchies. Use typed errors (implement `error` interface) with a `Type()`
  method for `On error type` matching. The types are: `ScriptError` (general), `CommandError`, `AssertionError`,
  `ExitError` (control flow — like Kotlin's `Break`), `MissingExpectedError`.
- **ExitError / Break propagation**: `Exit` command throws a special error that propagates up the call stack. Control
  flow commands (`If`, `When`, `ForEach`, `Do`) must NOT catch it — they must let it propagate. The TypeScript port had
  a bug where `script.run()` caught `Break` but control flow branches called `run()` instead of `runCommands()`.

### Dependencies

Minimal external dependencies needed:
- `gopkg.in/yaml.v3` — YAML parsing
- `github.com/itchyny/gojq` or similar — if implementing JSON path queries
- No HTTP framework needed — `net/http` covers client and server
- No CLI framework needed — hand-roll argument parsing (it's a simple state machine)
- `github.com/manifoldco/promptui` or `github.com/charmbracelet/bubbletea` for interactive prompts (Level 5)

### Project structure suggestion

```
specscript-go/
├── cmd/spec-go/main.go          # CLI entry point
├── pkg/
│   ├── types/                   # JsonValue type aliases, helpers
│   ├── language/
│   │   ├── context.go           # Script context
│   │   ├── script.go            # Script parsing and execution
│   │   ├── variables.go         # Variable resolution
│   │   ├── conditions.go        # Condition evaluation
│   │   ├── eval.go              # Eval expression parsing
│   │   ├── command.go           # CommandHandler interface, registry, dispatch
│   │   └── resolve.go          # The resolve pipeline
│   ├── commands/
│   │   ├── core.go              # Print, Output, As, Assignment, Do, Exit, Error
│   │   ├── testing.go           # Assert, Expected, TestCase, CodeExample
│   │   ├── control_flow.go      # If, When, ForEach, Repeat
│   │   ├── data.go              # Add, Find, Replace, Sort, etc.
│   │   ├── errors.go            # OnError, OnErrorType
│   │   ├── files.go             # ReadFile, WriteFile, TempFile, RunScript
│   │   ├── shell.go             # Shell, Cli
│   │   ├── http.go              # GET/POST/PUT/PATCH/DELETE, server commands
│   │   ├── prompt.go            # Prompt, PromptObject, Confirm
│   │   └── util.go              # Json, Text, Base64, Wait, ParseYaml
│   ├── markdown/
│   │   ├── scanner.go           # Line-by-line state machine
│   │   └── converter.go         # Blocks → Script
│   └── util/
│       ├── yaml.go              # YAML helpers
│       └── json_patch.go        # RFC 6902
├── test/
│   └── spec_runner_test.go      # Reads specification/ and runs tests
└── go.mod
```

## Common Pitfalls (from TypeScript experience)

1. **Don't resolve object keys** — only values. This breaks `Add to`, `Assignment`, and any command that interprets keys
   as variable names.
2. **`MissingNode` chaining** — `${obj.missing.nested}` should return empty string, not crash. Kotlin chains
   `MissingNode` objects; design your path navigation to handle missing intermediate segments gracefully.
3. **Null handling during auto-iteration** — Kotlin drops null results; TypeScript preserves them. The spec doesn't
   define this. Pick one and document it.
4. **YAML integer-vs-string ambiguity** — YAML parses `400` as integer, `"400"` as string. Error types, HTTP status
   codes, and other string-ish values may arrive as integers. Coerce where needed.
5. **Block scalar trailing newlines** — Different YAML parsers differ on whether block scalars include a trailing
   newline. Test with the `Temp file` → `Read file` roundtrip early.
6. **Circular imports** — Separate command registration from command definition. Register all commands in `main()` or an
   `init()` function, not at package init time.
7. **Shared session mutation** — `Run script` creates a child context that shares `Session` by reference. Changes to
   session in the child (server instances, answers, etc.) must be visible to the parent.
