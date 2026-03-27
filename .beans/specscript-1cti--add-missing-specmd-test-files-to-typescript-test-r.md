---
# specscript-1cti
title: Add missing spec.md test files to TypeScript test runner
status: completed
type: task
priority: normal
created_at: 2026-03-27T07:16:37Z
updated_at: 2026-03-27T07:40:15Z
---

Add category 2 (spec.md for already-implemented commands) and category 4 (language spec.md files) to the TypeScript test runner's hardcoded file lists.

## Tasks
- [x] Add category 2 spec.md files (control-flow, data-manipulation, errors, testing, util, variables, script-info)
- [x] Add category 4 language spec.md files
- [x] Run tests and fix any failures
- [x] Skip files/sections that need unimplemented commands (Prompt, Types)

## Summary of Changes

Added 49 new spec.md test files to the TypeScript test runner across two categories:
- Category 2: 41 command reference docs (LEVEL_1_MD_FILES) covering control-flow, data-manipulation, errors, testing, util, and variables
- Category 4: 8 language and script-info docs (added to LEVEL_2_TEST_FILES and LEVEL_3_MD_FILES)

Fixed 3 bugs along the way:
1. Expected error command: string value was incorrectly matched against error message instead of being used as the MissingExpectedError message
2. Shared context input variable leaking between test sections: added reset of error and input variable before each section
3. SKIP_TESTS false positives: changed to qualified path format to prevent cross-file matches

Result: 341 passing tests (up from 227 baseline), 20 skipped, 0 failures.
