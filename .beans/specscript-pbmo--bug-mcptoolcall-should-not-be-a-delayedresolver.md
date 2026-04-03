---
# specscript-pbmo
title: 'Bug: McpToolCall should not be a DelayedResolver'
status: completed
type: bug
priority: normal
created_at: 2026-04-03T06:23:41Z
updated_at: 2026-04-03T06:59:38Z
---

McpToolCall implements DelayedResolver in Kotlin but never calls resolve(). This means variables like ${state} in the input block are never resolved. There is no reason for McpToolCall to be a DelayedResolver — it's a regular command, not a meta-command like ForEach. Fix: remove DelayedResolver from McpToolCall in Kotlin.

## Summary of Changes\n\nRemoved DelayedResolver from McpToolCall in Kotlin. This was causing variables like ${state} in the input block to not be resolved. All 505 specification tests pass.

## Summary of Changes\n\nRemoved DelayedResolver from McpToolCall — it never called resolve() and had no reason to be a delayed resolver. Included in the squash commit 42ae002 on main.
