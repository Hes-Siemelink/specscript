---
# specscript-8g4l
title: 'Phase 1: SCRIPT_DIR variable'
status: completed
type: feature
priority: high
created_at: 2026-03-21T08:54:42Z
updated_at: 2026-03-21T09:19:34Z
parent: specscript-gd1h
---

Make ${SCRIPT_DIR} available as a first-class SpecScript variable everywhere, not just in Shell commands.

## Tasks
- [x] Move draft spec to specification/ directory and finalize
- [x] Get spec confirmed
- [x] Implement in Kotlin
- [x] Run tests

## Summary of Changes

- Added SCRIPT_DIR as a first-class SpecScript variable in FileContext.init block
- Added SCRIPT_DIR_VARIABLE constant to ScriptContext.kt
- Restructured Variables.spec.md: added Built-in variables table at top, SCRIPT_DIR section at bottom with executable examples
- Added SCRIPT_DIR tests to Variables tests.spec.yaml
- Fixed SCRIPT_DIR for .spec.md code examples in TestUtil.kt to point to original spec file directory
