---
# specscript-ho63
title: 'Phase 4: Connection inheritance (caller wins)'
status: completed
type: feature
priority: high
created_at: 2026-03-21T08:54:48Z
updated_at: 2026-03-21T13:08:09Z
parent: specscript-gd1h
---

When script A calls script B, A's connection definitions take precedence over B's.

## Tasks
- [x] Move draft spec to specification/ directory and finalize
- [x] Get spec confirmed
- [x] Implement in Kotlin
- [x] Run tests

## Summary of Changes

Connection inheritance implemented with "first one wins" semantics. When a parent script calls a child script via Run script, the parent's connection definitions (from .directory-info.yaml) are propagated to the child context. If the child's directory defines the same connection name, the parent's definition takes precedence.

Implementation: two files changed.
- FileContext.kt: secondary constructor propagates parent connections into session["connect-to.overrides"] using putIfAbsent
- ConnectTo.kt: checks overrides map before local directory connections

All 470+ specification tests pass.
