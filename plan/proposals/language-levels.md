# Language Levels: A Buildable Path for SpecScript Implementations

## Motivation

SpecScript has 80 commands and 85 spec files. Porting all of this to TypeScript, Go, or Rust before calling it
"functional" guarantees nothing ever ships. We need a layered decomposition that lets a new implementation be useful at
each stage — and testable against the specification at that stage.

The levels below are ordered by **bootstrap dependency**, not by importance. Each level builds on the previous ones and
adds a coherent capability.

## Levels

### Level 0: Core Runtime

**What you're building:** A YAML-to-command pipeline. Parse YAML, resolve variables, dispatch commands.

**Language features:**
- YAML multi-document parsing (`---` separators)
- Top-level arrays
- Command dispatch by name (case-insensitive matching)
- Variable storage and `${...}` resolution (string interpolation + full node replacement)
- Path navigation in variables (`${book.chapters[0].title}`, bracket notation)
- `output` variable (set after each command that returns a result)
- Eval syntax (`/CommandName` inline command execution within data)
- Array auto-mapping (if a non-list handler receives an array, each element is processed individually)
- `DelayedResolver` interface (some commands receive raw YAML without variable expansion)

**Commands (17):**

| Command | Why it's here |
|---|---|
| Print | You need hello world |
| Output | Set script output / assign structured data |
| As | Named variables |
| Do | Command grouping, sub-scripts |
| Script info | Script metadata (no-op at runtime, but needed for the data model) |
| Input parameters | Script input (simple form) |
| Input schema | Script input (JSON Schema form) |
| Assert equals | You need to test |
| Assert that | Conditional assertions |
| Expected output | Verify command output |
| Expected console output | Verify printed text |
| Expected error | Verify error conditions |
| Test case | Organize tests in a file |
| Code example | Same, for spec docs |
| Answers | Mock user prompts in tests |
| Error | Throw errors |
| Exit | Early return |

Note: `Assert that` pulls in the **Conditions** system (`is`, `is not`, `contains`, `matches`, comparison operators).
This is a deliberate choice — conditions are a core language feature, not a control-flow add-on. They're needed for
testing from day one.

**External dependencies:** YAML parser, JSON tree model. That's it.

**On Eval:** Yes, Eval is a funny one at Level 0. It _is_ the poor man's LISP-in-YAML — `/CommandName` as inline
function calls. But it's baked into the resolution pipeline (`eval()` runs on every command's data before variable
substitution), so it can't be deferred. Any implementation must handle it from the start, even if it's initially a
pass-through for unknown commands.

---

### Level 1: Control Flow and Data

**What you're building:** A complete scripting language for data manipulation.

**Additions:**

| Command | Why |
|---|---|
| If | Branching |
| When | Multi-branch |
| For each | Iteration |
| Repeat | Loops with conditions |
| On error | Error handling |
| On error type | Typed error handling |
| Find | JSON path navigation |
| Add | Combine data structures |
| Add to | Mutate variables |
| Append | Append to output |
| Fields | Extract fields |
| Replace | String/data replacement |
| Size | Collection size |
| Sort | Sort collections |
| Values | Extract values from objects |
| Json | Convert to JSON node |
| Text | Convert to text |
| Print Json | Print as JSON |
| Parse Yaml | Parse YAML strings |
| Base64 encode | Encoding |
| Base64 decode | Decoding |
| Wait | Timing |

**What you can do:** Full data processing scripts. Transform, filter, aggregate JSON/YAML data. Handle errors
gracefully.

---

### Level 2: Markdown Documents

**What you're building:** The ability to run `.spec.md` files — executable documentation.

This is promoted from Level 5 in the previous draft because **it unlocks self-testing against the specification**. Until
an implementation can parse Markdown, it can only test against `.spec.yaml` files, which cover edge cases but not the
main spec documents.

**Required language features:**
- Markdown scanner (parse code blocks from `.spec.md` files)
- Block type classification: `yaml specscript`, `hidden yaml specscript`, `output`, `answers`, header, quote
- Section splitting (each `# Header` becomes a test case)

**What you're NOT building yet:** `shell cli`, `shell`, `yaml file=` blocks. These block types exist in the Markdown
format but they invoke Level 3+ commands. The Markdown parser must recognize them (to not crash), but it can skip/ignore
blocks that reference unimplemented commands. A few spec files have sections that use `file=` or `shell cli` — those
sections will fail at this level, which is fine. The pure `yaml specscript` + `output` sections all pass.

**What you can do:** Run the specification's `.spec.md` files as tests. Self-validate against the spec.

---

### Level 3: Files, Shell, and Script Composition

**What you're building:** An automation tool that interacts with the OS.

**Additions:**

| Command | Why |
|---|---|
| Read file | File I/O |
| Write file | File I/O |
| Temp file | Temporary files (unlocks `yaml file=` blocks in Markdown) |
| Run script | Call other `.spec.yaml` files |
| Shell | Execute OS commands |
| Cli | Run spec CLI commands programmatically |

**Required language features:**
- File system access
- Process spawning
- Script-to-script calling (new context with shared `session`)
- Local file commands (files in same directory become callable commands)
- Directory scanning and `specscript-config.yaml`
- `SCRIPT_HOME`, `SCRIPT_TEMP_DIR`, `env` built-in variables
- Markdown block types `shell`, `shell cli`, `yaml file=` now fully functional

**What you can do:** After this level, _all_ Markdown block types work. Every spec file can run fully. Automation
scripts, build tools, deployment scripts.

---

### Level 4: HTTP

**What you're building:** An API testing and integration tool.

**Additions:**

| Command | Why |
|---|---|
| GET | HTTP methods |
| POST | |
| PUT | |
| PATCH | |
| DELETE | |
| Http request defaults | Connection defaults |
| Http server | Start test servers |
| Http endpoint | Define endpoints |
| Stop http server | Cleanup |

**Required language features:**
- HTTP client library
- HTTP server library (for test servers and for running the sample server used in spec tests)
- Session state for connection defaults and server lifecycle

**What you can do:** API testing, webhook handling, integration testing. The "Postman replacement" level.

---

### Level 5: User Interaction and Connections

**What you're building:** An interactive CLI tool framework.

**Additions:**

| Command | Why |
|---|---|
| Prompt | Ask user for input |
| Prompt object | Multi-field prompts |
| Confirm | Yes/no questions |
| Connect to | Named connections |
| Credentials | Credential management |
| Create/Delete/Get/GetAll/Set default credentials | Credential CRUD |
| Check type | Type checking |
| Validate schema | JSON Schema validation |

**Required language features:**
- Terminal UI library (interactive prompts)
- Credential storage (YAML file-based)
- Type system (`TypeRegistry`)
- JSON Schema validation library

**What you can do:** Build interactive CLI tools with persistent credentials and typed inputs.

---

### Level 6: Pluggable Modules

**What you're building:** Platform integrations, each as an independent module.

| Module | Commands | External dependency |
|---|---|---|
| **SQLite** | SQLite, SQLite defaults, Store | SQLite driver |
| **MCP** | Mcp server, Mcp tool, Mcp tool call, Mcp prompt, Mcp resource, Stop mcp server | MCP SDK |

These are independent of each other. They preview the pluggable commands pattern.

**Ghost level — Pluggable Commands:** A future architectural change where Levels 0–1 become the kernel and everything
from Level 3 onward registers through a plugin interface. This isn't a level to implement — it's a refactoring that
would happen _after_ at least two language implementations exist and we understand the real plugin boundary. The level
system is designed to make that boundary obvious: Level 0–1 is pure data, everything else adds I/O. The plugin interface
would formalize the `CommandHandler` registration that `CommandLibrary.kt` does today. Mentioning it here so we don't
design ourselves into a corner, but it's not a prerequisite for anything.

---

## Dependency Graph

```
Level 0: Core Runtime (17 commands)
    │
Level 1: Control Flow + Data (+22 = 39)
    │
Level 2: Markdown Documents (+0 commands, but new runtime capability)
    │
    ├──────────────────┐
    ▼                  ▼
Level 3: Files/Shell   Level 4: HTTP              (independent)
    │                  │
    ├──────────────────┘
    ▼
Level 5: User Interaction + Connections
    │
    ├─────────┬─────────┐
    ▼         ▼         ▼
Level 6a:  Level 6b:   ...                        (pluggable modules)
SQLite     MCP         (future)
```

Levels 3 and 4 are **independent** — you can do HTTP before files, or files before HTTP. Level 5 needs file I/O
(credentials storage) so it requires Level 3. The Markdown capability (Level 2) only needs Levels 0–1 for the core
`yaml specscript` + `output` blocks; `shell cli` and `file=` blocks become functional at Level 3.

---

## Testing Strategy

This is the critical question: how does a TypeScript or Go implementation validate itself against the specification
_before_ it can run `.spec.md` files?

### The bootstrap problem

The specification is written in `.spec.md` files. To run those, you need Markdown parsing (Level 2). But at Level 0,
you only have a YAML runtime. So how do you test Level 0?

### Solution: Three-phase testing

**Phase 1: Cross-runtime test runner (Levels 0–1)**

The Kotlin implementation acts as the **test oracle**. It already has `spec --test` which can run any `.spec.yaml` or
`.spec.md` file and report pass/fail. For a new implementation at Level 0–1:

1. The new implementation exposes a CLI: `specscript-ts run script.spec.yaml` (or equivalent).
2. A test harness (could be a shell script, could be the Kotlin `spec` tool itself) discovers the `.spec.yaml` test
   files for the target level.
3. For each test file, it invokes the new implementation and compares stdout/stderr against expected output.

This is simple and language-agnostic. The test harness doesn't need to understand SpecScript — it just runs a binary
and checks output. The expected outputs are embedded in the spec files themselves (`Expected console output`,
`Expected output`, `Assert equals`).

Concretely, the harness would:
- Parse each `.spec.yaml` test file to extract test cases (split at `Test case` or `Tests` commands)
- Run each test case through the target implementation
- Check the exit code (0 = pass, non-zero = fail)
- Report results

This harness is ~100 lines of code in any language. Or we build it as a SpecScript tool: `spec --test-external
<binary> <spec-directory>`.

**Phase 2: Self-testing (Level 2+)**

Once the implementation can parse Markdown, it can run the `.spec.md` files directly. Now it validates itself. At this
point, the Kotlin oracle is no longer needed — the spec files _are_ the tests.

However, some `.spec.md` sections use Level 3+ features (`file=`, `shell cli`, `Run script`). These sections will
fail until Level 3 is implemented. The test runner must tolerate partial failures and report which sections passed.

**Phase 3: Full conformance (Level 3+)**

With files, shell, and HTTP available, all spec tests pass. The implementation is fully self-validating.

### Which spec files are clean per level?

I analyzed all 67 spec and test files for Levels 0–1. Here's what actually depends on higher-level features:

| File | Higher-level dependency | What it uses |
|---|---|---|
| `SpecScript Yaml Scripts.spec.md` | Level 3 | `file=` blocks, `shell cli`, `Run script` |
| `Variables.spec.md` | Level 3 | `file=`, `Run script`, `Read file` |
| `Testing.spec.md` | Level 3 | `shell cli` |
| `Variables tests.spec.yaml` | Level 3 | `Temp file` |
| `Input schema.spec.md` | Level 3 | `file=`, `shell cli` |
| `Input parameters.spec.md` | Level 3 | `file=`, `shell cli` |
| `Script info.spec.md` | Level 3 | `file=`, `shell cli` |
| `For each tests.spec.yaml` | Level 3 | `Read file` |

**60 out of 67 files (90%) are clean at Level 0–1.** The contaminated files use `file=` and `shell cli` to
demonstrate CLI invocation (e.g., `spec --help greeting.spec.yaml`) — showcase sections, not core functionality. The
core assertions in those files still use `yaml specscript` blocks and work fine.

### Test infrastructure per level

| Level | Test source | Runner | Notes |
|---|---|---|---|
| 0 | `.spec.yaml` files from testing/, variables/, script-info/tests/, util/Print, errors/Error, control-flow/Do+Exit | External harness | 60 clean files. Harness runs target binary per test case. |
| 1 | + control-flow/, data-manipulation/, errors/, util/ `.spec.yaml` and `.spec.md` files | External harness | Same harness, more files. |
| 2 | All `.spec.md` files (partial — `yaml specscript` + `output` blocks only) | Self-hosted | Implementation runs its own spec files. Sections with `file=`/`shell cli` skip or fail gracefully. |
| 3 | All spec files, all block types | Self-hosted | Full `.spec.md` execution including `file=`, `shell cli`, `shell`. |
| 4 | + HTTP spec files | Self-hosted | Sample server on `localhost:2525` needed. |
| 5 | + user-interaction/, connections/ | Self-hosted | `Answers` mocking for prompt tests. |
| 6 | + db/, ai/mcp/ | Self-hosted | Module-specific dependencies. |

---

## Specification Directory Organization

### Current structure

The spec directory is organized by command group:

```
specification/
├── language/           ← core language features (7 spec files)
├── cli/                ← CLI tool usage (3 spec files)
└── commands/
    ├── core/
    │   ├── testing/    ← Assert, Expected output, Test case, ...
    │   ├── variables/  ← As, Output, Assignment
    │   ├── control-flow/
    │   ├── data-manipulation/
    │   ├── errors/
    │   ├── files/
    │   ├── http/
    │   ├── shell/
    │   ├── user-interaction/
    │   ├── connections/
    │   ├── schema/
    │   ├── types/
    │   └── db/
    └── ai/mcp/
```

### How to add levels without making it awkward

**Don't reorganize the directory structure.** Moving files into `level-0/`, `level-1/` directories would break all
cross-references, lose the logical grouping by functionality, and make the spec harder to read as documentation.

Instead, add a **level manifest** — a single YAML file that maps spec files to levels:

```yaml
# specification/levels.yaml

levels:
  - name: Core Runtime
    level: 0
    spec-files:
      - language/SpecScript Yaml Scripts.spec.md
      - language/Variables.spec.md
      - language/Eval syntax.spec.md
      - commands/core/testing/**
      - commands/core/variables/**
      - commands/core/script-info/**
      - commands/core/util/Print.spec.md
      - commands/core/errors/Error.spec.md
      - commands/core/control-flow/Do.spec.md
      - commands/core/control-flow/Exit.spec.md

  - name: Control Flow and Data
    level: 1
    spec-files:
      - commands/core/control-flow/**
      - commands/core/data-manipulation/**
      - commands/core/errors/**
      - commands/core/util/**

  - name: Markdown Documents
    level: 2
    spec-files:
      - language/SpecScript Markdown Documents.spec.md

  - name: Files, Shell, Script Composition
    level: 3
    spec-files:
      - commands/core/files/**
      - commands/core/shell/**
      - language/Organizing SpecScript files in directories.spec.md
      - language/Testing.spec.md
      - cli/**

  - name: HTTP
    level: 4
    spec-files:
      - commands/core/http/**

  - name: User Interaction and Connections
    level: 5
    spec-files:
      - commands/core/user-interaction/**
      - commands/core/connections/**
      - commands/core/schema/**
      - commands/core/types/**

  - name: SQLite
    level: 6
    spec-files:
      - commands/core/db/**

  - name: MCP
    level: 6
    spec-files:
      - commands/ai/mcp/**
```

This manifest serves multiple purposes:
1. **Test filtering:** `spec --test --level 1 specification/` runs only Level 0–1 tests.
2. **Conformance reporting:** An implementation declares its level; the test runner reports pass/fail per level.
3. **Documentation:** Each spec file gets a level badge in its rendered output (optional).
4. **No file moves:** The directory structure stays logical and readable.

### What a spec file looks like with levels

Nothing changes in the spec files themselves. The level is an external annotation, not an inline marker. This keeps
spec documents clean and avoids cluttering every file with metadata.

The only place levels appear is:
1. `specification/levels.yaml` — the manifest
2. The test runner — filters by level
3. Optionally, the READMEs — a table showing which level each command belongs to

### Handling cross-level contamination in spec files

Seven `.spec.md` files have sections that use Level 3 features (mainly `file=` and `shell cli` to demonstrate CLI
invocation). Options:

1. **Accept partial failures.** At Level 0–1, the Markdown parser skips `file=` and `shell cli` blocks. Sections that
   _only_ contain those blocks produce empty tests. Sections that mix `yaml specscript` with `shell cli` fail on the
   `shell cli` part but pass the `yaml specscript` part. The test runner reports "5/7 sections passed" rather than
   outright failure.

2. **Split contaminated sections.** Move the CLI-showcase sections into a separate file (e.g.,
   `Script info CLI.spec.md`) that lives at Level 3. The main `Script info.spec.md` stays at Level 0. This is clean
   but creates more files.

3. **Use the manifest's section-level annotations.** Extend `levels.yaml` to annotate individual sections:
   ```yaml
   - language/SpecScript Yaml Scripts.spec.md:
       default-level: 0
       sections:
         "Script info": 0
         "Defining script input": 3   # uses file= and shell cli
   ```

Recommendation: option 1 for now (accept partial failures), option 2 if it becomes confusing. Option 3 is
over-engineering at this stage.

---

## Command Count Per Level

| Level | New Commands | Cumulative | % of Total |
|---|---|---|---|
| 0 | 17 | 17 | 21% |
| 1 | 22 | 39 | 49% |
| 2 | 0 | 39 | 49% |
| 3 | 6 | 45 | 56% |
| 4 | 9 | 54 | 68% |
| 5 | 12 | 66 | 83% |
| 6 | 9 | 75 | 94% |

---

## Multi-repo note

Out of scope, but: the level structure suggests a **shared specification repo** (this one) with per-language
implementation repos that declare which level they target. The `levels.yaml` manifest and spec files become a
conformance suite. Monorepo vs multi-repo is an organizational choice, not a technical one — the spec is
runtime-agnostic either way.

---

## Relation to Pluggable Commands

This level system is a **precursor** to pluggable commands, not a replacement:

1. **Levels 0–1 define the kernel** — the runtime that must exist in every implementation.
2. **Levels 3–6 are all "plugins"** conceptually — they add commands with external dependencies (file system, HTTP,
   SQLite, MCP).
3. Level 2 (Markdown) is a **runtime capability**, not a plugin — it's part of the core interpreter.
4. A future pluggable command system would formalize the `CommandHandler` registration that `CommandLibrary.kt` does
   today. The level system identifies the natural plugin boundary.

The ghost level for pluggable commands sits between Level 2 and Level 3 architecturally — it's the moment where
"register new commands at runtime" becomes the mechanism, and Levels 3+ become the first consumers. But it's a
refactoring, not a feature level, so it doesn't appear in the numbered sequence.
