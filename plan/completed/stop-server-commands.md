# Proposal: Dedicated Stop Commands for Servers

## Problem

Both `Http server` and `Mcp server` use a `stop: true` flag to stop a running server. This overloads the "start"
command with "stop" semantics, which is unintuitive — you write the same command name to do the opposite thing.

Current:

```yaml
Http server:
  name: my-server
  stop: true
```

```yaml
Mcp server:
  name: my-server
  stop: true
```

This requires the schema to allow `stop` alongside `name` but without `endpoints`/`tools`/etc, creating an awkward
union type. It also means the `execute()` method has a branching `if stop` at the top, mixing two responsibilities.

## Proposed Solution

Introduce dedicated stop commands that take the server name as a simple string value.

### Name options

| Option                  | Pro                                    | Con                                      |
|-------------------------|----------------------------------------|------------------------------------------|
| `Stop http server`      | Reads naturally as an action           | "Stop" prefix is unique in the language  |
| `Http server stop`      | Groups with `Http server` alphabetically and conceptually | Reads a bit odd as English |
| `Stop server`           | Short, clean                           | Ambiguous — which server type?           |

### Usage

```yaml
Stop http server: my-api
Stop mcp server: my-tools
```

### The casing question

The stop command raises a casing question: should it be `Stop http server` or `Stop Http server` or `Stop HTTP server`?
This touches a broader inconsistency in the language.

#### Current casing patterns (57 commands)

| Pattern              | Count | Examples                                          |
|----------------------|-------|---------------------------------------------------|
| ALL CAPS             | 5     | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`            |
| Single word title    | 20    | `Do`, `If`, `Exit`, `Output`, `Find`               |
| Sentence case        | 25    | `For each`, `Read file`, `On error`, `Assert that` |
| Title-cased acronyms | 7     | `Http server`, `Mcp server`, `Json patch`, `Parse Yaml` |
| Proper noun          | 2     | `SQLite`, `SQLite defaults`                        |

#### How acronyms are handled today

| Acronym | In verb form | In compound names    | Consistent? |
|---------|-------------|----------------------|-------------|
| HTTP    | `GET`, `POST` (ALL CAPS) | `Http server`, `Http request defaults` | No |
| MCP     | —           | `Mcp server`, `Mcp tool`, `Mcp tool call` | Within MCP, yes |
| JSON    | —           | `Json patch`, `Print Json`, `Json` | Yes |
| YAML    | —           | `Parse Yaml`         | Yes (1 instance) |
| SQL     | —           | `SQLite` (proper noun preserved) | Special case |

The dominant pattern for multi-word commands is **sentence case** — first word capitalized, rest lowercase. Acronyms
are treated as regular words in this scheme: `Http`, `Mcp`, `Json`, `Yaml`. The exception is HTTP verbs (`GET`, `POST`),
which are ALL CAPS because they are protocol method names used as standalone command names, not because of a general
acronym-uppercasing rule.

#### Command matching is case-sensitive

Commands are resolved via exact `Map[key]` lookup — no normalization. Writing `HTTP server` when the registered name is
`Http server` throws `Unknown command`. There is an acknowledged TODO in CommandLibrary.kt:

```kotlin
// TODO Store commands in canonical form: all lower case and spaces
```

This TODO proposes normalizing to all-lowercase at registration and lookup time, which would make commands
case-insensitive.

#### Three options for the stop command casing

**Option A: Follow existing sentence-case convention**

```yaml
Stop http server: my-api
Stop mcp server: my-tools
```

Pro: Consistent with the established pattern (`Http server`, `Mcp tool`, `Json patch`). No new convention.
Con: `http` in the middle of a command looks slightly odd to people used to seeing `HTTP`.

**Option B: Uppercase acronyms**

```yaml
Stop HTTP server: my-api
Stop MCP server: my-tools
```

Pro: Acronyms look "correct" to most developers.
Con: Inconsistent with every existing compound command (`Http server`, `Mcp tool`, `Json patch`). Creates pressure to
rename all existing commands. And if the start command stays `Http server`, having the stop command be `Stop HTTP server`
is immediately confusing.

**Option C: Implement case-insensitive matching**

Normalize command names to lowercase at registration and lookup. Users can write `Http server`, `HTTP server`,
`http server` — all resolve the same.

Pro: Eliminates the entire class of casing mistakes. The TODO already exists. Removes the need to pick a "correct"
casing for new commands.
Con: Invites inconsistency in user scripts (different files may use different casing). Documentation must still pick a
canonical display form. Requires a migration for all registered command names.

### Recommendation

**Option A (sentence case) now, with Option C (case-insensitive) as a separate follow-up.**

Rationale:

1. Sentence case is the established convention for 32 of 34 multi-word commands. Breaking it for one new command creates
   more confusion than it solves.
2. Case-insensitive matching is a good idea on its own merits but is a broader change that affects all 57 commands, the
   matching logic, and documentation. It should be a separate tracked item.
3. The stop commands should ship with the current convention. If/when case-insensitive matching lands, users who wrote
   `Stop HTTP server` will just work.

Final command names:

```yaml
Stop http server: my-api
Stop mcp server: my-tools
```

### Schema

Both commands take a single string value (the server name). No object form needed.

```yaml
# Stop http server.schema.yaml
$schema: https://json-schema.org/draft/2020-12/schema
type: string
```

### Impact

**Http server:**
- Remove `stop` property from `Http server` schema and data class
- New command `Stop http server` registered in CommandLibrary
- Simple `ValueHandler` implementation that calls `HttpServer.stop(name)`

**Mcp server:**
- Same treatment: remove `stop` from schema and data class
- New command `Stop mcp server` that calls `McpServer.stopServer(name)`

**Spec files, samples, tests:**
- All `Http server: { name: X, stop: true }` become `Stop http server: X`
- All `Mcp server: { name: X, stop: true }` become `Stop mcp server: X`
- Net reduction in YAML verbosity (3 lines → 1 line per stop)

**Test suite:**
- `SpecScriptTestSuite.kt` already calls `HttpServer.stop("sample-server")` directly in Kotlin — no change needed there

### Implementation Scope

Two small `ValueHandler` command objects (~10 lines each), schema removal from the parent commands, and search-replace
across spec/sample files. Low risk, high readability improvement.

### Follow-up: Case-insensitive command matching

Tracked separately. Normalize to all-lowercase in `commandMap()` and `getCommandHandler()`. Pick a canonical display
form for documentation (sentence case). Non-breaking — existing scripts with correct casing continue to work.
