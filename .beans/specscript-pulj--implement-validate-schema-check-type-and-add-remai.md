---
# specscript-pulj
title: Implement Check type command and add remaining XS spec test files
status: completed
type: task
priority: normal
created_at: 2026-03-27T08:55:54Z
updated_at: 2026-03-27T09:49:59Z
---

## Tasks

- [x] Add Before all tests.spec.md to LEVEL_4_MD_FILES
- [x] Add CLI spec.md files (Command line options, Running SpecScript files) to test runner
- [x] Implement Validate schema command — SKIPPED (needs full JSON Schema lib)
- [x] Implement Check type command
- [x] Add Validate schema.spec.md — SKIPPED
- [x] Add Types.spec.md to LEVEL_5_MD_FILES
- [x] Add Validate tests.spec.yaml — SKIPPED
- [x] Add Type tests.spec.yaml to LEVEL_5_TEST_FILES
- [x] Remove Schema tests.spec.yaml from SKIP_FILES — SKIPPED
- [x] Run full test suite — 0 failures (364 passed, 21 skipped)

## Summary of Changes

Implemented Check type command and type system (TypeRegistry, type validation, types.yaml loading). Added 5 spec files to the test runner (Types.spec.md, Type tests.spec.yaml, Before all tests.spec.md, Command line options.spec.md, Running SpecScript files.spec.md). Fixed CLI --help bug where invokeDirectory short-circuited on help flag instead of recursing into subcommands. Skipped Validate schema (needs full JSON Schema lib). Final: 364 passed, 21 skipped, 0 failures.
