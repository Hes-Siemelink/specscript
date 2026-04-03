# TypeScript Implementation Plan — Level 0

## Scope

This plan covers **Level 0 only**: the core runtime with 17 commands. Level 1 (control flow + data) and Level 2
(Markdown) are separate efforts that build on this foundation.

**Exit criteria for Level 0:**
- All 17 commands implemented
- `pnpm test` runs Level 0 spec tests from `$SPECSCRIPT_HOME/specification/` and they pass
- `specscript-ts script.spec.yaml` runs a .spec.yaml file from the command line

---

## Monorepo Layout

Keep the Kotlin build at root (avoid a disruptive restructuring). Add the TypeScript project as a subdirectory.

```
specscript/
├── specification/          ← shared, the source of truth
├── samples/                ← shared examples
├── src/                    ← Kotlin implementation (unchanged)
├── build.gradle.kts        ← Kotlin build (unchanged)
├── typescript/             ← NEW: TypeScript implementation
│   ├── package.json
│   ├── tsconfig.json
│   ├── vitest.config.ts
│   ├── src/
│   │   ├── cli.ts          ← entry point
│   │   ├── language/       ← core runtime
│   │   ├── commands/       ← command implementations
│   │   └── util/           ← YAML/JSON utilities
│   └── test/
│       ├── spec-runner.ts  ← runs .spec.yaml tests against the TS runtime
│       └── unit/           ← unit tests
├── .gitignore              ← add node_modules, dist
└── plan/
```

The TypeScript project locates shared assets via `SPECSCRIPT_HOME` environment variable, which points at the repo root.
In the monorepo, it defaults to `..` (or auto-detected by walking up from `cwd` looking for a `specification/`
directory). This gives:

- `$SPECSCRIPT_HOME/specification/` — the spec files and conformance tests
- `$SPECSCRIPT_HOME/samples/` — sample scripts (needed at Level 4 for sample-server)

This keeps the TS project decoupled from its physical location in the repo and makes a future multi-repo split trivial
— just point `SPECSCRIPT_HOME` at wherever you cloned the spec repo.

### Why not move Kotlin into a subdirectory?

- Breaks all existing CI, scripts, the `spec` alias, contributor muscle memory
- The Kotlin project IS the root project — it owns the specification
- A nested TS project is additive, not disruptive

---

## Tech Stack

| Concern | Choice | Why |
|---|---|---|
| Runtime | Node 22+ | LTS, native test runner available, stable ESM |
| Language | TypeScript 5.x | Strict mode |
| YAML | `yaml` (npm) | YAML 1.2 compliant, multi-document support, mature |
| JSON tree | Native objects + thin wrapper | TS objects ARE JSON. Thin `JsonValue` type with helpers for type guards and path navigation |
| Test framework | Vitest | Fast, good DX, parallel execution |
| Build | `tsup` or `esbuild` | Fast bundling to single file for CLI distribution |
| CLI parsing | Hand-rolled | At Level 0 we only need `specscript-ts <file>` and `specscript-ts --test <dir>` |
| Package manager | `pnpm` | Fast, strict, monorepo-friendly if we add more packages later |

### On the JSON tree model

TypeScript has a natural advantage here: JavaScript objects _are_ JSON. No need for Jackson's `JsonNode` class
hierarchy. But we need type safety and path navigation, so:

```typescript
// A SpecScript value — any valid JSON/YAML value
type JsonValue = string | number | boolean | null | JsonValue[] | JsonObject
type JsonObject = { [key: string]: JsonValue }

// Helper functions, not a class hierarchy
function isObject(v: JsonValue): v is JsonObject { ... }
function isArray(v: JsonValue): v is JsonValue[] { ... }
function isString(v: JsonValue): v is string { ... }
function getPath(root: JsonValue, path: string): JsonValue | undefined { ... }
```

This is idiomatic TypeScript — no wrapper classes, just type guards and utility functions. The entire Jackson
`ObjectNode`/`ArrayNode`/`ValueNode` hierarchy collapses to native types.

---

## Build Order

### Phase 1: Skeleton (day 1)

Set up the project structure, build tooling, and a minimal CLI that can run `Print: Hello world`.

```
typescript/
├── package.json
├── tsconfig.json
├── vitest.config.ts
├── src/
│   ├── cli.ts                    ← main(), parse args, load file, run
│   ├── util/
│   │   └── yaml.ts               ← parse YAML, multi-document support
│   ├── language/
│   │   ├── types.ts               ← JsonValue, Command, Script types
│   │   ├── script.ts              ← Script: parse commands from YAML docs, run loop
│   │   ├── context.ts             ← ScriptContext: variables map, command lookup
│   │   ├── variables.ts           ← ${...} resolution, path navigation
│   │   ├── eval.ts                ← /CommandName inline evaluation
│   │   ├── conditions.ts          ← is, is not, contains, matches, comparisons
│   │   ├── command-handler.ts     ← CommandHandler interface + handler type variants
│   │   └── command-execution.ts   ← resolve + dispatch pipeline
│   └── commands/
│       └── print.ts               ← Print command
```

**Milestone:** `npx specscript-ts hello.spec.yaml` prints "Hello world".

### Phase 2: Core commands (days 2–3)

Implement the remaining 16 Level 0 commands. Order matters — each group unlocks testing capability:

1. **Output, As** — variables work, scripts can produce data
2. **Do** — command grouping, sub-script execution
3. **Assert equals, Expected output** — you can now write self-checking tests
4. **Assert that** — conditional assertions (pulls in the Conditions system)
5. **Expected console output** — verify Print output (needs stdout capture)
6. **Expected error, Error** — error path testing
7. **Test case, Code example, Answers** — test organization
8. **Exit** — early return
9. **Script info, Input parameters, Input schema** — script metadata and input

**Milestone:** Can run Level 0 `.spec.yaml` test files and they pass.

### Phase 3: Test harness (day 3–4)

Build the spec test runner that discovers and runs `.spec.yaml` files:

```typescript
// test/spec-runner.ts
// 1. Recursively find .spec.yaml files in $SPECSCRIPT_HOME/specification/
// 2. Filter to Level 0 files (hardcoded list or levels.yaml)
// 3. Parse each file, split at Test case / Tests commands
// 4. Run each test case through the TS runtime
// 5. Report pass/fail with expected vs actual on failure
```

Wire this into Vitest so `pnpm test` runs both unit tests and spec tests. Use a custom test factory to generate one
Vitest test per spec test case (same pattern as the Kotlin `@TestFactory`).

**Milestone:** `pnpm test` runs the Level 0 spec tests and reports results.

---

## Architecture Decisions

### Command registration

```typescript
// command-registry.ts
const registry = new Map<string, CommandHandler>()

function register(handler: CommandHandler): void {
  registry.set(handler.name.toLowerCase(), handler)
}

function getHandler(name: string): CommandHandler | undefined {
  return registry.get(name.toLowerCase())
}
```

Level 0 commands are registered at startup. When pluggable commands arrive, this becomes the plugin entry point. No
changes needed — just call `register()` from plugin init code.

### Handler types

```typescript
interface CommandHandler {
  name: string
  delayedResolver?: boolean    // replaces DelayedResolver marker interface
  errorHandler?: boolean       // replaces ErrorHandler marker interface
  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined
}
```

No need for separate `ValueHandler`/`ObjectHandler`/`ArrayHandler` interfaces. TypeScript's dynamic typing means the
`execute` function receives whatever JSON value was in the YAML. The handler checks the type itself. Simpler, fewer
abstractions.

### Variable resolution

```typescript
// variables.ts
const VARIABLE_REGEX = /\$\{([^}]+)\}/g

function resolveVariables(node: JsonValue, variables: Map<string, JsonValue>): JsonValue {
  if (typeof node === 'string') {
    // Full replacement: "${varName}" → variable value (preserves type)
    // Interpolation: "Hello ${name}!" → string substitution
  }
  if (isObject(node)) { /* recurse properties */ }
  if (isArray(node)) { /* recurse elements */ }
  return node
}
```

### Script context

```typescript
interface ScriptContext {
  variables: Map<string, JsonValue>
  session: Map<string, unknown>      // cross-script shared state
  scriptFile: string                  // path to current file
  output: JsonValue | undefined       // shortcut to variables.get('output')
  error: SpecScriptError | undefined  // current unhandled error
  getHandler(name: string): CommandHandler
}
```

---

## What NOT to Build at Level 0

- **CLI features:** No `--help`, `--output`, `--interactive` flags beyond `--test`. Just `specscript-ts <file>`.
- **Markdown parsing:** That's Level 2.
- **Control flow:** If, When, For each, Repeat — that's Level 1.
- **Data manipulation:** Add, Find, Replace, etc. — Level 1.
- **Directory scanning:** No local file commands, no `specscript-config.yaml`. Level 3.
- **File I/O commands:** No Read file, Write file, Temp file. Level 3.
- **HTTP:** Level 4.
- **npm publishing:** Not until at least Level 1.

---

## Level 0 Spec Files

These are the spec files the test harness runs at Level 0. All are clean (no Level 3+ dependencies):

**Command specs and tests:**
- `specification/commands/core/testing/**` (all 11 spec files + Assert tests.spec.yaml)
- `specification/commands/core/variables/**` (3 spec files + 3 test files)
- `specification/commands/core/script-info/tests/Input schema tests.spec.yaml`
- `specification/commands/core/script-info/tests/Input parameters tests.spec.yaml`
- `specification/commands/core/util/Print.spec.md`
- `specification/commands/core/errors/Error.spec.md`
- `specification/commands/core/control-flow/Do.spec.md`
- `specification/commands/core/control-flow/Exit.spec.md`

**Language specs (partially — only `yaml specscript` + `output` blocks):**
- `specification/language/Eval syntax.spec.md`

**Excluded from Level 0** (clean files but contain Level 1 commands):
- `specification/language/Variables.spec.md` — uses If, For each in examples

**Excluded from Level 0** (use Level 3 features in their examples):
- `specification/commands/core/script-info/Script info.spec.md` — uses `file=`, `shell cli`
- `specification/commands/core/script-info/Input schema.spec.md` — uses `file=`, `shell cli`
- `specification/commands/core/script-info/Input parameters.spec.md` — uses `file=`, `shell cli`
- `specification/language/SpecScript Yaml Scripts.spec.md` — uses `file=`, `shell cli`, `Run script`

These main spec files are still _readable_ documentation at Level 0, just not fully executable as tests until Level 3.
The `.spec.yaml` test files in `tests/` subdirectories cover the same commands without higher-level dependencies.

---

## Timeline Estimate

| Phase | Duration | Cumulative | What's working |
|---|---|---|---|
| 1: Skeleton | 1 day | 1 day | `Print: Hello world` runs |
| 2: Level 0 commands | 2 days | 3 days | All 17 commands, self-checking tests |
| 3: Test harness | 1 day | 4 days | Spec test runner, Level 0 test files green |

4 working days to a tested Level 0. After that, Level 1 (control flow + data, ~3 days) and Level 2 (Markdown, ~2 days)
are separate plans.

---

## Open Questions

1. **YAML output formatting.** The Kotlin implementation uses Jackson's `MINIMIZE_QUOTES` and
   `WRITE_DOC_START_MARKER=false`. The `yaml` npm package has equivalent options but the output formatting may differ
   slightly. Spec tests that check exact YAML output strings may need attention.

2. **Error message format.** Spec tests that check error messages (via `Expected error`) will fail if the TS
   implementation produces different wording. Need to decide: match Kotlin error messages exactly, or treat error
   messages as implementation-specific and only test error _types_.

3. **`levels.yaml` manifest.** Should we create this now (in the specification) so the TS test runner can filter by
   level from day one? Probably yes — it's a small file and immediately useful.

---

## What Comes Next

**Level 1: Control Flow + Data** (~3 days) — 22 commands. Unlocks full data processing. No new infrastructure, just
more command implementations using the same patterns from Level 0.

**Level 2: Markdown Documents** (~2 days) — No new commands. Adds the Markdown scanner so the implementation can run
`.spec.md` files and self-test against the specification. This is the "proving the spec is language-agnostic" milestone.
