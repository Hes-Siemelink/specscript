---
# specscript-rn86
title: Wildcard path matching for Http server endpoints
status: todo
type: bug
created_at: 2026-04-03T07:18:55Z
updated_at: 2026-04-03T07:18:55Z
---

The '*' wildcard syntax in endpoint paths doesn't work — it gets passed to Ktor as-is instead of being normalized to Ktor's '{...}' tailcard syntax. Two proxy samples use '*' (recording-proxy, replaying-proxy) while the mock server sample had to use the Ktor-specific '{...}' directly. Fix: normalize '*' to '{...}' in normalizePath, document wildcard paths in the Http server spec.
