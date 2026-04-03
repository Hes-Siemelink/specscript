---
# specscript-lrs6
title: Http endpoint description and inputSchema (Proposal B)
status: completed
type: feature
priority: normal
created_at: 2026-04-03T09:01:37Z
updated_at: 2026-04-03T10:54:57Z
---

Add description and inputSchema fields to Http server endpoint MethodHandler, with derivation from referenced script files. Mirrors MCP tool pattern.

## Plan

- [x] Update spec: add description and inputSchema to Http server endpoint schema and spec docs
- [x] Update schema: Http server.schema.yaml MethodHandler
- [x] Implement Kotlin: derive description and inputSchema from script files in HttpServer.kt
- [x] Implement TypeScript: same derivation in http-server.ts
- [x] Run all tests (Kotlin spec + unit, TypeScript vitest)
- [x] Make MCP tool description derivable from Script info

## Summary of Changes

Added description and inputSchema fields to Http server endpoint handlers, with automatic derivation from referenced script files (Script info for description, Input schema for inputSchema). Extended the same description derivation to MCP tools — description is now optional when backed by a script with Script info. Both Kotlin and TypeScript implementations updated and passing all tests.

## Revision\n\nHttp endpoint metadata (description/inputSchema) was reverted — no consumer exists. Shared derivation infrastructure removed. Only MCP tool description derivation remains, as a local concern in McpServer.
