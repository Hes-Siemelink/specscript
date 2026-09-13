# TypeScript: SQLite, SQLite defaults, Store commands — Port Report

## Dependency choice: better-sqlite3

Chose better-sqlite3 (synchronous C++ addon) over alternatives:
- **bun:sqlite** — built-in to Bun but not available under Node/vitest. Since tests run with vitest (Node), this was not viable without reworking the test runner.
- **sql.js** — pure JS (Emscripten-compiled SQLite), no native compilation needed. Would have avoided the native addon build step but is slower and has a different API shape.

better-sqlite3's synchronous API is a natural match for the Kotlin JDBC-style code. The rest of the TypeScript codebase uses async/await, but the commands are already async at the interface level — the synchronous DB calls inside are an implementation detail.

Trade-off: adds a native addon dependency that requires compilation on install. This could be a problem in environments without a C toolchain (e.g., some CI containers, Vercel edge). If that becomes an issue, sql.js is the fallback.

## Bugs found and fixed

### Path resolution (both SQLite and Store)

Both Kotlin commands passed `file:` directly to JDBC, resolving against process CWD rather than `context.workingDir`. The TypeScript port exposed this because vitest's CWD differs from the script's working directory. Fixed in both implementations to resolve against `context.workingDir`.

### Store multi-column select (both implementations)

The `doJsonQuery` function had the object-creation inside the column loop, producing one single-key object per column per row (e.g., `[{name: 'Alice'}, {age: 16}]`). Fixed to produce one object per row (e.g., `[{name: 'Alice', age: 16}]`). Added a spec test for multi-column select that was previously missing.

## Open issues

SQL injection: both Kotlin and TypeScript have the same FIXME — no prepared statements for user-provided SQL. The Store command's insert does use a prepared statement for the JSON value, but table names and where clauses are interpolated directly.
