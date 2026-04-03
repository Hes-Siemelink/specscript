---
# specscript-3zr4
title: Revert Http endpoint metadata and shared derivation infrastructure
status: completed
type: task
priority: normal
created_at: 2026-04-03T10:52:39Z
updated_at: 2026-04-03T10:54:52Z
---

Strip Http endpoint description/inputSchema (no consumer). Revert shared deriveFromScript/HandlerInputSchema/DerivedHandlerMetadata back to local McpServer concerns. Keep MCP description derivation as simple local change.

## Plan\n\n- [x] Revert Http server spec (remove Endpoint metadata section)\n- [x] Revert Http server schema (remove description, inputSchema, InputSchema from MethodHandler)\n- [x] Revert HttpServer.kt (remove description/inputSchema from MethodHandlerData)\n- [x] Revert http-server.ts (remove description/inputSchema from RouteHandler/HandlerData)\n- [x] Revert HandlerInfo.kt (remove DerivedHandlerMetadata, HandlerInputSchema, deriveFromScript)\n- [x] Move derivation logic back into McpServer.kt as local concern (with description support)\n- [x] Move derivation logic into mcp-server.ts as local concern (with description support)\n- [x] Run all tests

## Summary of Changes

Reverted all Http endpoint metadata (description/inputSchema) — no consumer exists. Reverted shared derivation infrastructure from HandlerInfo.kt. Kept MCP description derivation as a local concern: deriveFromScript in McpServer.kt and mcp-server.ts returns both description and inputSchema. All tests pass.
