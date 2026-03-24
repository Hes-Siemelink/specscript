---
# specscript-gy4c
title: Implement Stop http server and Stop mcp server commands
status: completed
type: feature
priority: normal
created_at: 2026-03-24T06:24:19Z
updated_at: 2026-03-24T06:35:34Z
---

Replace stop: true flag with dedicated Stop http server / Stop mcp server commands. Sentence case (Option A).

- [x] Write Stop http server spec section in Http server.spec.md
- [x] Write Stop mcp server spec section in Mcp server.spec.md
- [x] Create Stop http server schema
- [x] Create Stop mcp server schema
- [x] Implement StopHttpServer.kt
- [x] Implement StopMcpServer.kt
- [x] Register both in CommandLibrary
- [x] Remove stop property from Http server schema and data class
- [x] Remove stop property from Mcp server schema and data class
- [x] Update all spec examples (Http server)
- [x] Update all spec examples (Mcp server)
- [x] Update samples
- [x] Update Http server tests
- [x] Build and run all tests


## Summary of Changes

Implemented dedicated Stop http server and Stop mcp server commands as ValueHandler implementations. Removed stop property from both server schemas, data classes, and execute methods. Updated all spec files (Http server, Mcp server, Mcp tool, Mcp resource, Mcp prompt, Mcp tool call, overview), test files, and samples to use the new commands. All 476 specification tests pass.
