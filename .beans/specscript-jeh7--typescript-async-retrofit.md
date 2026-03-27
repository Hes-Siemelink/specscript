---
# specscript-jeh7
title: TypeScript async retrofit
status: completed
type: task
priority: normal
created_at: 2026-03-26T14:54:56Z
updated_at: 2026-03-26T15:07:45Z
---

Convert the TypeScript SpecScript engine from synchronous to async execution. Phases: 1) Core async conversion (mechanical), 2) Simplify HTTP client, 3) Simplify HTTP server, 4) Update test harness, 5) Verification.

## Todo

- [x] Phase 1: Core async conversion — CommandHandler, dispatch pipeline, script runner, all 58 handlers
- [x] Phase 2: Simplify HTTP client — replace spawnSync with await fetch()
- [x] Phase 3: Simplify HTTP server — replace three-process fork with in-process createServer()
- [x] Phase 4: Update test harness
- [x] Phase 5: Verify all 226 tests pass

## Summary of Changes

Converted the entire TypeScript SpecScript engine from synchronous to async execution across all 5 phases:

- Phase 1: Made CommandHandler.execute() return Promise, converted all 58 command handlers, core pipeline (eval, resolve, runCommand, script.run), and all callers to async/await
- Phase 2: Replaced spawnSync child-process HTTP client with direct await fetch()
- Phase 3: Replaced forked child-process HTTP server with in-process createServer() — no more fork, spawnSync, temp dirs, ready files, or IPC
- Phase 4: Updated test harness — all beforeAll/afterAll/it callbacks async, all script.run() awaited
- Phase 5: Fixed race condition in Level 4 test suite (two describe blocks each starting/stopping the sample server on port 2525 independently; merged into single describe with shared lifecycle)

Final result: 226/226 tests passing, 7 skipped (same baseline as before retrofit).
