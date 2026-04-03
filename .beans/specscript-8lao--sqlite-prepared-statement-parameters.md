---
# specscript-8lao
title: SQLite prepared statement parameters
status: completed
type: feature
priority: normal
created_at: 2026-04-03T21:00:24Z
updated_at: 2026-04-03T21:14:52Z
---

Make SQLite/Store commands use prepared statements for variable references, write specs, update samples

## Tasks

- [x] Write spec for SQLite command (specification/commands/core/db/SQLite.spec.md)
- [x] Write spec for Store command (specification/commands/core/db/Store.spec.md)
- [x] Implement DelayedResolver + prepared statements in Kotlin (SQLite.kt)
- [x] Implement DelayedResolver + prepared statements in TypeScript (sqlite.ts)
- [x] Update sample scripts to remove Replace/As quote-escaping boilerplate
- [x] Run all tests (specification + unit) in both implementations

## Summary of Changes

SQLite command now uses prepared statement parameters for SQL injection prevention. Variable references wrapped in single quotes (`'${var}'`) are converted to `?` placeholders with resolved values as parameters. Unquoted `${var}` references are still resolved inline (for table names, column names, etc.).

Changes:
- Kotlin: SQLite.kt rewritten as DelayedResolver, new PreparedSql.kt utility
- TypeScript: sqlite.ts updated with delayedResolver flag and prepareSql function
- Spec files: SQLite.spec.md, SQLite defaults.spec.md, Store.spec.md added to specification/commands/core/db/
- Test cases: 4 new prepared statement tests in SQLite tests.spec.yaml
- Sample scripts: Removed Replace/As quote-escaping boilerplate from 6 files (goals-app, contacts, ticket-db)
- All 460 TypeScript tests and 440+ Kotlin specification tests pass
