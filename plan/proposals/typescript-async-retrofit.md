# Proposal: Retrofit TypeScript SpecScript Engine to Async

## Problem

The SpecScript TypeScript engine is synchronous. Every `execute()` call blocks the Node.js event
loop. This forces three workarounds that add complexity and overhead:

1. **HTTP client** — spawns a child process per request to run `fetch()`, because `fetch()` is
   async-only. Each request pays ~70ms subprocess overhead.
2. **HTTP server** — runs in a forked child process with file-based ready signaling, because an
   in-process server can't handle requests while the engine blocks the event loop.
3. **Shell** — uses `execSync`, which is fine for now but prevents future streaming output.

An async engine eliminates all three workarounds: `await fetch()` directly, run the server
in-process, and optionally use `execAsync` for streaming.

## Before and After

### CommandHandler interface

```typescript
// BEFORE (synchronous)
export interface CommandHandler {
  name: string
  delayedResolver?: boolean
  errorHandler?: boolean
  handlesLists?: boolean
  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined
}
```

```typescript
// AFTER (async)
export interface CommandHandler {
  name: string
  delayedResolver?: boolean
  errorHandler?: boolean
  handlesLists?: boolean
  execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined>
}
```

### A simple command (Output)

```typescript
// BEFORE
export const Output: CommandHandler = {
  name: 'Output',
  execute(data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    return data ?? null
  },
}
```

```typescript
// AFTER — identical logic, just async signature
export const Output: CommandHandler = {
  name: 'Output',
  async execute(data: JsonValue, _context: ScriptContext): Promise<JsonValue | undefined> {
    return data ?? null
  },
}
```

Most command handlers are pure data transformations. The async keyword is ceremony — no awaits
needed. ~40 of 58 handlers fall into this category.

### A command that benefits (GET)

```typescript
// BEFORE — spawns a subprocess to run fetch(), blocks on spawnSync
export const GetCommand: CommandHandler = {
  name: 'GET',
  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    if (isString(data)) return processValueRequest(data, context, 'GET')
    if (isObject(data)) return processObjectRequest(data, context, 'GET')
    throw new CommandFormatError('GET: expected a URL string or object')
  },
}

// In http-client.ts — the actual I/O
function executeSyncRequest(url, method, headers, body, ...): JsonValue | undefined {
  const script = `(async () => { const r = await fetch(...); ... })()`
  const result = spawnSync(process.execPath, ['-e', script, requestData], { ... })
  // parse result from child process stdout
}
```

```typescript
// AFTER — direct fetch(), no subprocess
export const GetCommand: CommandHandler = {
  name: 'GET',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isString(data)) return processValueRequest(data, context, 'GET')
    if (isObject(data)) return processObjectRequest(data, context, 'GET')
    throw new CommandFormatError('GET: expected a URL string or object')
  },
}

// In http-client.ts — direct fetch
async function executeRequest(url, method, headers, body, ...): Promise<JsonValue | undefined> {
  const opts: RequestInit = { method, headers }
  if (body !== undefined) opts.body = body
  const response = await fetch(url, opts)
  const bodyText = await response.text()
  // parse response directly — no subprocess, no temp files
}
```

### A command with sub-scripts (For each)

```typescript
// BEFORE
execute(data: JsonValue, context: ScriptContext): JsonValue {
  // ...setup...
  for (const item of enumerated) {
    context.variables.set(loopVar, item)
    const bodyCopy = deepCopy(body)
    const script = Script.fromData(bodyCopy)
    const result = script.run(context)       // synchronous
    // ...collect results...
  }
  return results
}
```

```typescript
// AFTER
async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue> {
  // ...setup (unchanged)...
  for (const item of enumerated) {
    context.variables.set(loopVar, item)
    const bodyCopy = deepCopy(body)
    const script = Script.fromData(bodyCopy)
    const result = await script.run(context)  // await added
    // ...collect results (unchanged)...
  }
  return results
}
```

### The script runner

```typescript
// BEFORE
run(context: ScriptContext): JsonValue | undefined {
  this.runCommands(context)
  return context.output
}

runCommands(context: ScriptContext): void {
  for (const command of this.commands) {
    const handler = context.getCommandHandler(command.name)
    if (context.error && !handler.errorHandler) continue
    runCommand(handler, command.data, context)     // synchronous
  }
}
```

```typescript
// AFTER
async run(context: ScriptContext): Promise<JsonValue | undefined> {
  await this.runCommands(context)
  return context.output
}

async runCommands(context: ScriptContext): Promise<void> {
  for (const command of this.commands) {
    const handler = context.getCommandHandler(command.name)
    if (context.error && !handler.errorHandler) continue
    await runCommand(handler, command.data, context)  // await added
  }
}
```

### The dispatch pipeline

```typescript
// BEFORE
export function runCommand(handler, rawData, context): JsonValue | undefined { ... }
function runSingleCommand(handler, rawData, context): JsonValue | undefined { ... }
function handleCommand(handler, data, context): JsonValue | undefined { ... }
export function resolve(data, context): JsonValue { ... }
```

```typescript
// AFTER
export async function runCommand(handler, rawData, context): Promise<JsonValue | undefined> { ... }
async function runSingleCommand(handler, rawData, context): Promise<JsonValue | undefined> { ... }
async function handleCommand(handler, data, context): Promise<JsonValue | undefined> { ... }
export async function resolve(data, context): Promise<JsonValue> { ... }  // because evalExpressions calls runCommand
```

### HTTP server (the big simplification)

```typescript
// BEFORE — three-process architecture (parent + fork + sub-subprocess)
// http-server.ts: ~350 lines including:
//   - CommonJS server script written to temp file
//   - fork() to start child process
//   - File-based ready signal with existsSync() polling
//   - IPC messages for route registration
//   - execFileSync sub-subprocess for script handlers
```

```typescript
// AFTER — in-process server, ~80 lines
import { createServer } from 'node:http'

async execute(data, context): Promise<JsonValue | undefined> {
  const server = createServer((req, res) => {
    // Route matching + handler dispatch
    // For script handlers: run SpecScript directly (same event loop)
  })
  await new Promise<void>(resolve => server.listen(port, resolve))
  // Store server reference in session for Stop http server
}
```

The entire `http-server.ts` shrinks from ~350 lines to ~80.

## Work Decomposition

### Phase 1: Core async conversion (mechanical)

Convert the execution pipeline and all command handlers to async. This is almost entirely
mechanical — add `async`/`await` keywords. No behavioral changes.

Files changed:

| File | Change |
|---|---|
| `command-handler.ts` | `execute()` returns `Promise<JsonValue \| undefined>` |
| `command-execution.ts` | `runCommand`, `resolve`, `handleCommand` become async |
| `script.ts` | `run()`, `runCommands()` become async |
| `eval.ts` | `evalExpressions()` becomes async (calls `runCommand`) |
| `context.ts` | No interface change (getCommandHandler stays sync) |
| `cli.ts` | `executeFile()`, `main()` become async |
| All 14 command files | Add `async` to `execute()`, `await` sub-script calls |

Estimated: ~58 handlers to update, ~40 of which are trivial (add `async`, no `await` needed).

### Phase 2: Simplify HTTP client

Replace `spawnSync`-based fetch with direct `await fetch()`. Delete the subprocess scaffolding.

Files changed:

| File | Change |
|---|---|
| `http-client.ts` | Replace `executeSyncRequest` with `executeRequest` using `await fetch()` |

Estimated: net reduction of ~50 lines.

### Phase 3: Simplify HTTP server

Replace the three-process fork architecture with an in-process `http.createServer()`.
Script handlers call the engine directly instead of spawning sub-subprocesses.

Files changed:

| File | Change |
|---|---|
| `http-server.ts` | Rewrite: in-process server, direct handler dispatch |

Estimated: net reduction of ~270 lines.

### Phase 4: Update test harness

The test harness calls `script.run()` and `executeFile()` — these become async.

Files changed:

| File | Change |
|---|---|
| `spec-runner.test.ts` | Test functions become async, `await script.run()` |

This is also mechanical — vitest already supports async test functions.

### Phase 5: Verification

- Run all 226 tests, confirm same pass/skip/fail counts
- Verify no regressions in HTTP client behavior (timeouts, error codes, response parsing)
- Verify HTTP server behavior (startup, routing, script handlers, variable interpolation)
- Check that `npx tsc` compiles cleanly

## Risks

### 1. Behavioral change in error propagation (Medium risk)

Async functions wrap thrown errors in rejected Promises. The current error handling relies on
synchronous try/catch chains:

```
script.run() → runCommands() → runCommand() → handleCommand() → execute()
```

With async, each level needs explicit `try/catch` or the error becomes an unhandled rejection.
The existing try/catch blocks in `runCommands()` and `handleCommand()` should work with `await`,
but edge cases (e.g., errors thrown before the first `await` in an async function) need testing.

**Mitigation:** Phase 1 is purely mechanical — same try/catch, just with `await`. Run the full
test suite after Phase 1 before proceeding.

### 2. eval.ts recursive execution during resolve (Medium risk)

`evalExpressions()` calls `runCommand()` synchronously during the resolve phase. Making this
async means `resolve()` becomes async, which propagates up through `runSingleCommand()`. This
is the deepest change — every call to `resolve()` must be awaited, including the explicit calls
in `ForEach`, `Repeat`, `If`, and `When` (delayed resolver commands that call `resolve()`
manually).

**Mitigation:** Grep for all `resolve(` calls and ensure each is awaited. There are ~8 call
sites.

### 3. HTTP server request handler async boundary (Low-Medium risk)

Node.js `http.createServer()` expects a synchronous callback signature
`(req, res) => void`. Running SpecScript commands inside that callback requires async:

```typescript
createServer(async (req, res) => {
  const result = await script.run(context)
  res.end(JSON.stringify(result))
})
```

This works, but unhandled rejections in the callback won't automatically send error responses.
Each handler needs explicit error wrapping.

**Mitigation:** Wrap the handler body in try/catch, send 500 on unhandled errors.

### 4. Stack traces become harder to read (Low risk)

Async stack traces include `Promise` frames and may lose synchronous context. This makes
debugging command execution harder during development.

**Mitigation:** Node.js 21+ has good async stack traces by default. Not a practical issue.

### 5. Performance regression for pure-computation commands (Low risk)

Adding `async`/`await` to commands that don't need it (Output, Fields, Add, etc.) introduces
microtask overhead per command execution. For a script with 100 pure-data commands, this adds
~100 microtask queue cycles.

**Mitigation:** The overhead is negligible (< 1ms for 100 commands). The HTTP subprocess
overhead we're removing (~70ms per request) dwarfs this. Net performance is better.

### 6. The sub-subprocess for script handlers disappears (Low risk)

Currently, HTTP server script handlers run in an isolated sub-subprocess. Moving to in-process
execution means a misbehaving script handler could corrupt shared state. However, this matches
Kotlin's behavior — Ktor handlers run in-process there too.

**Mitigation:** None needed — matching Kotlin's architecture is correct.

## Recommendation

Execute phases 1 through 5 sequentially. Phase 1 is the bulk of the work but is mechanical
and testable in isolation — the test suite should pass identically after Phase 1 with no
behavioral changes. Phases 2 and 3 are the payoff: simpler code, fewer processes, better
performance. Phase 4 is trivial.

Total estimated effort: ~2 hours for an agent, mostly Phase 1 (touching 20+ files) and Phase 3
(server rewrite).
