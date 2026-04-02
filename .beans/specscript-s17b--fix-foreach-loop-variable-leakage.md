---
# specscript-s17b
title: Fix ForEach loop variable leakage
status: completed
type: bug
priority: normal
created_at: 2026-04-02T04:50:06Z
updated_at: 2026-04-02T04:57:23Z
---

Loop variable from For each leaks into parent scope after loop completes. Fix by saving/restoring the variable around the loop.

## Plan\n\n- [x] Update spec: add scoping note to For each.spec.md\n- [x] Add test: verify loop variable doesn't leak\n- [x] Fix Kotlin: save/restore in ForEach.kt\n- [x] Fix TypeScript: save/restore in control-flow.ts\n- [x] Run all tests

## Summary of Changes

Fixed For each loop variable leakage by saving the loop variable value before the loop and restoring it (or removing it) after the loop completes. Used try/finally to ensure cleanup even on error. Added spec section documenting scoping behavior. Test verifies that a pre-existing variable with the same name as the loop variable is restored after the loop.
