# TypeScript: SQLite, SQLite defaults, Store commands — Port Report

## Dependency choice: better-sqlite3

Chose better-sqlite3 (synchronous C++ addon) over alternatives:
- **bun:sqlite** — built-in to Bun but not available under Node/vitest. Since tests run with vitest (Node), this was not viable without reworking the test runner.
- **sql.js** — pure JS (Emscripten-compiled SQLite), no native compilation needed. Would have avoided the native addon build step but is slower and has a different API shape.

better-sqlite3's synchronous API is a natural match for the Kotlin JDBC-style code. The rest of the TypeScript codebase uses async/await, but the commands are already async at the interface level — the synchronous DB calls inside are an implementation detail.

Trade-off: adds a native addon dependency that requires compilation on install. This could be a problem in environments without a C toolchain (e.g., some CI containers, Vercel edge). If that becomes an issue, sql.js is the fallback.

## Path resolution divergence from Kotlin

The Kotlin implementation passes `sql.file` directly to JDBC (`jdbc:sqlite:${sql.file}`), which resolves relative paths against the JVM's working directory (process CWD). The TypeScript port explicitly resolves against `context.workingDir` using `resolve(context.workingDir, file)`.

This is necessary because vitest's CWD is `typescript/` while `context.workingDir` is the repo root. Without this, the Shell command's `rm -f out/sample.db` and the SQLite command's `new Database('out/sample.db')` would operate on different directories, causing "table already exists" errors. The Kotlin test runner presumably sets CWD = workingDir so the difference doesn't surface there.

This is arguably more correct than the Kotlin behavior — a command should use its context's working directory, not the process CWD. But it's a behavioral difference worth noting for the implementer guide.

## Store command: multi-column select produces flat array

The Store command's `doJsonQuery` mirrors the Kotlin behavior where multi-column selects produce a flat array of single-key objects rather than one object per row. For example, `select: [name, age]` on a row with name=Alice, age=30 produces `[{name: 'Alice'}, {age: 30}]` rather than `[{name: 'Alice', age: 30}]`. This matches the Kotlin implementation and the spec tests pass, but it's a surprising API shape. Not changing it — spec defines the behavior — but worth noting.

## What the reviewer should look at

1. The `update` parameter handling at sqlite.ts:72 — uses `?? []` to default. The Kotlin version gets this from the domain object's default. Same effect but different mechanism.
2. SQL injection: both Kotlin and TypeScript have the same FIXME — no prepared statements for user-provided SQL. The Store command's insert does use a prepared statement for the JSON value, but table names and where clauses are interpolated directly.
3. The lockfile: `typescript/package-lock.json` appeared as untracked. Previous lockfile was `bun.lock` — verify which package manager is canonical.
