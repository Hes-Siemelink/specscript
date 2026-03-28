# Spec-Level Alignment Analysis

How well do the specification documents align with the language levels system, and what should change?

## Current State

`specification/levels.yaml` assigns 142 spec files across 8 levels (0–6, with two Level 6 modules). A cross-reference
of every file against every command it uses reveals 29 instances of cross-level contamination across 18 files.

**90% of Level 0–1 files are clean.** The contamination is concentrated in a handful of files and follows clear
patterns.

## Types of Contamination

### 1. Testing Scaffolding (not real contamination)

Commands like `Assert equals`, `Expected output`, `Assert that`, and `Expected error` are Level 0. They appear
everywhere. Some Level 0 spec files also use Level 1 commands (`Add`, `Replace`, `For each`, `On error`) as part of
demonstrating their concept. For example:

- `Error.spec.md` (L0) uses `On error` (L1) — can't demonstrate Error without catching it
- `Output.spec.md` (L0) uses `Replace` (L1) and `For each` (L1) — shows output in different contexts
- `Assert that.spec.md` (L0) uses `If` (L1) in one section — shows condition reuse

**Verdict**: This is the L0/L1 boundary being slightly fuzzy, not a real problem. These commands are all in the "kernel"
(Levels 0–1). A test runner that implements Level 0 should expect a few sections to fail until Level 1 is also done.

### 2. CLI Showcase Sections (the main contamination vector)

The dominant pattern: Level 0 spec files use `yaml file=` blocks and `shell cli` blocks (both Level 3) to show how a
feature looks when invoked from the command line. Affected files:

| File (Level 0) | Contaminated Sections | What they show |
|---|---|---|
| `SpecScript Yaml Scripts.spec.md` | Hello world, Script info, Defining script input, Script output | How to run scripts from CLI |
| `Variables.spec.md` | Script output, Input variable, Script Input & Output, SCRIPT_HOME | Script I/O integration |
| `Script info.spec.md` | Hidden commands | CLI directory listing |
| `Input parameters.spec.md` | Cli help, Using types | CLI --help output |
| `Input schema.spec.md` | Cli help | CLI --help output |

These sections are **supplementary** — they demonstrate integration, not the core concept. The core concept is explained
in earlier sections using only Level 0 commands.

**Verdict**: These sections should be annotatable or splittable so that Level 0 implementations can skip them cleanly.

### 3. Inherent Cross-Level Dependencies (unavoidable)

Some specs reference higher-level commands because the concept *requires* them:

| File | Level | Uses | Why |
|---|---|---|---|
| `Answers.spec.md` | L0 | `Prompt` (L5) | Answers exists to mock Prompt |
| `Before all tests.spec.md` | L0 | `Http request defaults` (L4) | Example shows test setup for HTTP |
| `SpecScript Markdown Documents.spec.md` | L2 | `Shell`, `Read file`, `Prompt` (L3/L5) | Documents block types that invoke L3+ features |
| `Input schema.spec.md` | L0 | `Mcp tool` (L6) | Shows MCP tool compatibility |

**Verdict**: These need structural solutions — either section-level annotations or spec restructuring.

### 4. Test File Contamination

Three `.spec.yaml` test files reach into higher levels:

| File | Level | Uses | Why |
|---|---|---|---|
| `Variables tests.spec.yaml` | L0 | `Temp file` (L3) | Tests SCRIPT_HOME vs SCRIPT_TEMP_DIR |
| `For each tests.spec.yaml` | L1 | `Read file` (L3) | Tests iteration over file content |
| `Schema tests.spec.yaml` | L1 | `Validate schema` (L5) | Schema validation test |
| `Add tests.spec.yaml` | L1 | `Validate schema` (L5) | Schema validation for Add |
| `Exit tests.spec.yaml` | L0 | `If` (L1) | Tests Exit inside If branch |

**Verdict**: Test files should be strictly within their level. Move the contaminated tests to the appropriate level.

## Recommendations

### A. Move contaminated test cases to higher levels

These are clean, surgical moves with no impact on the spec documents:

| Move | From | To | Rationale |
|---|---|---|---|
| "SCRIPT_HOME is different from SCRIPT_TEMP_DIR" test | `Variables tests.spec.yaml` (L0) | New file in L3 tests, or append to `Read file tests.spec.yaml` | Uses `Temp file` |
| "For each with variable syntax in sample data" tests (2) | `For each tests.spec.yaml` (L1) | New L3 test file or append to `Run script tests.spec.yaml` | Uses `Read file` |
| "Schema validation" tests | `Schema tests.spec.yaml` (L1), `Add tests.spec.yaml` (L1) | New L5 test file | Uses `Validate schema` |
| "Exit inside If" test | `Exit tests.spec.yaml` (L0) | Move to L1 `If tests.spec.yaml` or keep at L0 | Uses `If` — minor, L0/L1 boundary |

The Exit/If case is borderline. `If` is Level 1, but it's testing `Exit` behavior (L0). Pragmatically, keep it at L0
since `Exit tests.spec.yaml` is specifically about Exit semantics, and any Level 0 implementation will hit Level 1
immediately after.

### B. Add section-level annotations to levels.yaml

For spec.md files with mixed-level sections, extend `levels.yaml` to support section-level overrides:

```yaml
spec-files:
  - language/SpecScript Yaml Scripts.spec.md:
      skip-sections:
        - "Hello world example"       # uses yaml file=, shell cli (L3)
        - "Defining script input"     # uses yaml file=, shell cli (L3)
        - "Script output"             # uses Run script (L3)
        - "The command sequence"      # uses Prompt (L5)
```

This is more maintainable than splitting files. The spec documents stay logically coherent, and each test runner
implementation can read the skip list from the manifest. The TypeScript implementation already does something similar
with hardcoded skip lists — this formalizes it.

Affected files and their skip-sections:

| File (assigned level) | Sections to skip | Reason |
|---|---|---|
| `SpecScript Yaml Scripts.spec.md` (L0) | Hello world example, Defining script input, Script output, The command sequence | L3/L5 commands |
| `Variables.spec.md` (L0) | Script output, Script Input & Output, SCRIPT_HOME | L3 commands |
| `Error.spec.md` (L0) | Basic usage, Error type and data | L1 commands (`On error`, `On error type`) |
| `Output.spec.md` (L0) | Basic usage, Output in For each | L1 commands (`Replace`, `For each`) |
| `Answers.spec.md` (L0) | Basic usage | L5 (`Prompt`) |
| `Before all tests.spec.md` (L0) | Basic usage | L4 (`Http request defaults`, `GET`) |
| `Assert that.spec.md` (L0) | Empty | L1 (`If`) |
| `Input schema.spec.md` (L0) | Cli help, Compatibility with MCP tool definitions | L3/L6 |
| `Input parameters.spec.md` (L0) | Cli help, Using types | L3 |
| `Script info.spec.md` (L0) | Hidden commands | L3 |
| `SpecScript Markdown Documents.spec.md` (L2) | Hidden code, Predefined answers, Helper files, Variables inside temp files, Shell commands, Setting the current directory, Options to show command and output, Invoking SpecScript itself | L3/L5 |

### C. Reconsider the Level 0 / Level 1 boundary

The current boundary puts all testing and variable commands at Level 0 and all control flow and data manipulation at
Level 1. But several Level 0 spec files *need* Level 1 commands to demonstrate their concepts:

- `Error.spec.md` needs `On error` / `On error type` — error handling is conceptually inseparable from error raising
- `Output.spec.md` needs `Replace` and `For each` to show output behavior in realistic contexts
- `Assert that.spec.md` needs `If` to show condition reuse
- `Exit tests.spec.yaml` needs `If` to test Exit inside branches

**Options**:

1. **Move `On error` and `On error type` to Level 0.** They are tightly coupled with `Error`. Error without error
   handling is not useful. This also moves their spec and test files.

2. **Move `If` to Level 0.** It's the simplest control flow command and is needed by many Level 0 tests. `When` stays
   at Level 1.

3. **Keep as-is, use skip-sections.** Accept that Level 0 is a "bare bones" level and Level 1 completes it. This is the
   status quo.

**Recommendation**: Option 1 (move error handling to L0). Option 2 is tempting but `If` depends on `resolve()` handling
conditions, which is architecturally the same as all other Level 1 commands. Error handling is different — `On error`
and `On error type` are structurally simple (they set a flag in context, catch exceptions) and are needed to demonstrate
`Error` meaningfully.

### D. Restructure the Before all tests example

`Before all tests.spec.md` is at Level 0 but its only example uses `Http request defaults` (L4) and `GET` (L4). The
concept of "Before all tests" is Level 0, but the example is Level 4.

**Fix**: Write a Level 0 example that uses only L0 commands (e.g., set a variable that tests reference). Keep the HTTP
example as a second example or move it to a test file at Level 4.

### E. Restructure the Answers example

`Answers.spec.md` is at Level 0 but uses `Prompt` (L5). The Answers command IS essential at Level 0 (it's the mechanism
for non-interactive testing via Input parameters), but demonstrating it with Prompt creates a Level 5 dependency.

**Fix**: Add a Level 0 example showing Answers with `Input parameters` (which is L0). Keep the Prompt example as a
second example. Something like:

```yaml
# Before the test, define answers for input parameters
Answers:
  name: Alice

# This script uses Input parameters which would prompt interactively
# but Answers provides the value non-interactively
Input parameters:
  name:
    description: Your name

Print: Hello, ${name}!
```

### F. Formalize the "kernel" concept

Levels 0 and 1 together form the SpecScript kernel — the minimal viable scripting language. The Level 0/1 split is an
implementation ordering concern, not a conceptual one. Document this:

```yaml
# In levels.yaml header:
# Levels 0 and 1 together form the "kernel" — the complete data manipulation and
# scripting language. Level 0 provides the execution pipeline and testing framework;
# Level 1 adds control flow and data commands. Most implementers should treat them
# as a single milestone.
```

This sets expectations: a Level 0 implementation will have some failing sections that clear up at Level 1. That's
by design, not a bug.

### G. Level 2 is inherently contaminated

`SpecScript Markdown Documents.spec.md` documents block types (`shell`, `shell cli`, `yaml file=`) that are Level 3
features. This contamination is **unfixable without splitting the file**, because the spec's job is to document all
Markdown block types.

**Options**:

1. **Split the file**: `SpecScript Markdown Documents.spec.md` (L2, covers `yaml specscript`, `output`, `answers`,
   `hidden`) and `SpecScript Markdown Advanced.spec.md` (L3, covers `yaml file=`, `shell`, `shell cli`). Clean but
   creates a somewhat artificial division.

2. **Accept it and use skip-sections.** The test runner skips L3+ sections when running at Level 2. The file stays
   logically coherent.

3. **Move the file to Level 3.** But then there's no Level 2 spec file at all, which defeats the purpose of the level.

**Recommendation**: Option 2. Use skip-sections in `levels.yaml`. The file is the spec's most important "how to write
specs" document and should not be split.

## Summary of Changes to levels.yaml

If all recommendations are adopted:

1. **Move `On error` and `On error type` from Level 1 to Level 0** (with their spec and test files)
2. **Move 5 contaminated test cases** to their correct levels
3. **Add `skip-sections` support** to `levels.yaml` for 11 spec.md files
4. **Rewrite `Before all tests.spec.md` example** to use only L0 commands
5. **Add L0 example to `Answers.spec.md`** using Input parameters instead of Prompt
6. **Add kernel concept** documentation to levels.yaml header
7. **Leave Level 2's Markdown Documents contamination** as-is with skip-sections

These changes reduce cross-level contamination from 29 instances to ~8 (all in spec.md showcase sections handled by
skip-sections), without reorganizing the directory structure.

## Recommendations for levels.yaml Itself

The file was thrown together before work started and served its purpose, but it has structural issues now that it's
proven useful as infrastructure.

### H. Distinguish command level from spec-file level

The file conflates two things: which level a *command* belongs to, and which level a *spec file* should be tested at.
These are different:

- `Answers` is a Level 0 command (needed for testing from day one)
- `Answers.spec.md` can only be fully tested at Level 5 (its example uses Prompt)

The TypeScript test runner discovered this the hard way — it doesn't follow levels.yaml because the file's "spec-files"
list doesn't account for this distinction. The TS runner places `.spec.md` files at the level where their *examples* can
actually run, not where their *command* is defined.

**Fix**: The skip-sections proposal (recommendation B) handles this. But the conceptual distinction should be documented
in the levels.yaml header: "A spec file is assigned to the level of the command it documents. Some files contain
sections that require higher-level commands — these are listed in skip-sections and will only pass at the higher level."

### I. Fix the `requires` field

Current `requires` values are inconsistent in what they mean:

| Level | Current | Actual dependency | Issue |
|---|---|---|---|
| Level 4 HTTP | `requires: [1]` | `[0]` is sufficient | HTTP commands don't need L1. One spec example uses `For each`, but that's a spec-file dependency, not a command dependency. |
| Level 6 MCP | `requires: [4]` | `[1]` or even `[0]` | MCP uses stdio/SSE transport, not HTTP commands. No MCP spec file uses any HTTP command. |

Levels 0–2 have no `requires` (implicit: cumulative). Level 3 has no `requires` (implicit: cumulative). Level 5
`requires: [3]` is correct (credentials use file system).

**Fix**: Either:
- Define `requires` as *command-level* dependency (what the implementation needs): `Level 4: [0]`, `Level 6 MCP: [0]`
- Define `requires` as *spec-test* dependency (what you need to run the tests): `Level 4: [1]`, `Level 6 MCP: [1]`
- Or drop `requires` and state the rule: "Levels are cumulative (0→1→2). Levels 3 and 4 are independent of each other
  but both require Level 1. Level 6 modules list their own dependencies."

**Recommendation**: Drop `requires` from individual levels. Replace with a dependency graph comment in the header:

```yaml
# Dependency graph:
#   0 → 1 → 2 (cumulative kernel + markdown)
#   2 → 3 (files, shell)
#   2 → 4 (http) — independent of 3
#   3 → 5 (user interaction, connections)
#   4 → 6/MCP
#   3 → 6/SQLite
```

This is clearer than scattered `requires` fields and prevents the "what does requires mean" ambiguity.

### J. Add per-command metadata

The `commands` list is a flat list of names. The TypeScript implementation reports repeatedly flagged that implementers
need to know each command's handler type. Add metadata:

```yaml
commands:
  - name: If
    handles-lists: false
    delayed-resolver: true
    error-handler: false
  - name: For each
    handles-lists: true
    delayed-resolver: true
    error-handler: false
```

This eliminates the need to reverse-engineer handler types from Kotlin source. It's the single most valuable addition
for a new implementer.

Alternatively, keep the commands list flat and put the metadata in a separate `command-metadata.yaml` file. Either way,
it needs to exist somewhere machine-readable.

### K. Track `ParameterData.schema.yaml`

`Conditions.schema.yaml` is tracked because it's a shared schema. `ParameterData.schema.yaml` is equally shared
(used by Input parameters, Prompt, Prompt object) and should also be tracked. Either track both or track neither —
current state is inconsistent.

### L. Add the `Assignment` / `${}` mapping

levels.yaml lists `Assignment` as a command, but the actual Kotlin registration name is `${}`. This is the only command
where the spec name and the registration name differ. Document the mapping to prevent confusion:

```yaml
commands:
  - Assignment  # registered as '${}'  in the command registry
```

### M. Consider making levels.yaml a proper spec

Right now levels.yaml is a plain YAML file with no validation. It could become a SpecScript spec file
(`Language levels.spec.md`) with executable tests that verify:

- Every spec file on disk appears in exactly one level
- Every registered command appears in exactly one level
- `skip-sections` entries reference sections that actually exist in the file

This would catch drift automatically as part of `./gradlew specificationTest`. However, this is a Level 3+ capability
(needs file system access to scan directories), so it can't self-test at lower levels. A unit test may be more practical.
