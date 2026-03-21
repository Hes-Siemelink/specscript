---
# specscript-lbx3
title: 'Phase 3: env property on Input schema'
status: completed
type: feature
priority: high
created_at: 2026-03-21T08:54:48Z
updated_at: 2026-03-21T09:57:55Z
parent: specscript-gd1h
blocked_by:
    - specscript-8g4l
---

Input parameters can declare an environment variable source via env: property.

## Tasks
- [x] Move draft spec to specification/ directory and finalize
- [x] Get spec confirmed  
- [x] Implement in Kotlin
- [x] Run tests

## Summary of Changes

env property added to Input schema. Parameters can declare env: VAR_NAME to read from environment variables. Resolution order: explicit input > env var > default > interactive prompt. Two spec examples added (basic env binding, fallback to default). All 469 spec tests pass.
