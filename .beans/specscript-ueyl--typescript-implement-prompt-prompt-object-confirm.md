---
# specscript-ueyl
title: 'TypeScript: Implement Prompt, Prompt object, Confirm commands'
status: completed
type: feature
priority: low
created_at: 2026-03-27T10:03:50Z
updated_at: 2026-03-27T11:43:21Z
---

Implement Prompt, Prompt object, Confirm commands and --interactive CLI behavior. See plan/proposals/typescript-prompt-interactive.md for full design.

## Tasks

- [x] Add @inquirer/prompts dependency
- [x] Create user-prompt.ts (TestPrompt + InquirerPrompt dispatch)
- [x] Create prompt.ts (Prompt command)
- [x] Create prompt-object.ts (Prompt object command)
- [x] Create confirm.ts (Confirm command)
- [x] Register commands in register.ts
- [x] Add interactive fallback to Input parameters (script-info.ts) — also add Answers check before error
- [x] Add interactive directory menu to CLI (cli.ts)
- [x] Add 6 spec files to test runner
- [x] Run full test suite — 0 failures (395 passed, 16 skipped)

## Summary of Changes

Implemented Prompt, Prompt object, and Confirm commands with TestPrompt simulation for spec tests. Added interactive fallback to Input parameters (recorded Answers check + doPrompt). Added interactive directory menu to CLI. All 395 tests pass.
