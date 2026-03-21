---
# specscript-1dhm
title: 'Phase 2: env namespace'
status: completed
type: feature
priority: high
created_at: 2026-03-21T08:54:48Z
updated_at: 2026-03-21T09:29:00Z
parent: specscript-gd1h
---

Expose OS environment variables as ${env.VAR_NAME} in SpecScript.

## Tasks
- [x] Move draft spec to specification/ directory and finalize
- [x] Get spec confirmed
- [x] Implement in Kotlin
- [x] Run tests

## Summary of Changes

- Added env variable backed by System.getenv() ObjectNode in FileContext.init
- Added ENV_VARIABLE constant to ScriptContext.kt
- Added Environment variables section to Variables.spec.md with 3 executable examples
- Dot-path resolution handles ${env.HOME} naturally (variable=env, path=.HOME)
