---
# specscript-zqkc
title: Investigate temp dir and test execution cleanup
status: completed
type: task
priority: normal
created_at: 2026-03-30T19:01:49Z
updated_at: 2026-03-30T19:43:12Z
---

Investigate how temp dirs, SCRIPT_HOME, and test execution interact. Propose cleanup options.

## Proposal Written\n\nProposal document: plan/proposals/temp-dir-test-execution-cleanup.md\n\nThree proposals analyzed (A: explicit test context with fallback resolution, B: minimal fix, C: rejected). Recommendation: Proposal A.

## Implementation Plan

- [x] Phase 1: Replaced with scriptHome approach — add scriptHome to ScriptContext
- [x] Phase 2: Simplified getCodeExamplesAsTests — uses scriptHome param instead of variable override
- [x] Phase 3: CLI test runner no longer uses JUnit types
- [x] Phase 4: Update TypeScript to match
- [x] All specification tests pass (506 Kotlin, 413 TypeScript)
- [x] All unit tests pass

## Summary of Changes\n\nAdded scriptHome as a first-class concept to ScriptContext, separating the original spec file location from the temp dir used during test execution. This eliminated the manual SCRIPT_HOME variable override hack in getCodeExamplesAsTests(), fixed a toRealPath() bug in ReadFile.kt, and removed JUnit dependencies from the CLI test runner path. Both Kotlin and TypeScript implementations updated and passing all tests.
