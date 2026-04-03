---
# specscript-rn86
title: Wildcard path matching for Http server endpoints
status: completed
type: bug
priority: normal
created_at: 2026-04-03T07:18:55Z
updated_at: 2026-04-03T08:44:29Z
---

The '*' wildcard syntax in endpoint paths doesn't work — it gets passed to Ktor as-is instead of being normalized to Ktor's '{...}' tailcard syntax. Two proxy samples use '*' (recording-proxy, replaying-proxy) while the mock server sample had to use the Ktor-specific '{...}' directly. Fix: normalize '*' to '{...}' in normalizePath, document wildcard paths in the Http server spec.

## Tasks\n\n- [x] Understand current normalizePath behavior and Ktor's tailcard syntax\n- [x] Fix normalizePath to translate '*' to '{...}'\n- [x] Update mock-server sample to use '*' instead of '{...}'\n- [x] Check all samples for consistency\n- [x] Run relevant tests\n- [x] Document wildcard paths in Http server spec

## Summary of Changes

- Fixed normalizePath in HttpServer.kt to translate "*" to Ktor's "{...}" tailcard syntax
- Updated 2 samples (mock-server, cloud-connector-tests) from "{...}" to "*"
- 2 existing samples (recording-proxy, replaying-proxy) already used "*" — now they work correctly
- Added Path patterns section to Http server.spec.md with executable examples for path parameters and wildcards
- All tests pass (unit, specification, full check)
