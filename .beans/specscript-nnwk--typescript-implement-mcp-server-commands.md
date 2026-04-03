---
# specscript-nnwk
title: 'TypeScript: Implement MCP server commands'
status: completed
type: feature
priority: deferred
created_at: 2026-03-27T10:04:03Z
updated_at: 2026-04-02T06:00:04Z
---

Implement MCP commands: Mcp server, Mcp tool, Mcp tool call, Mcp prompt, Mcp resource, Stop mcp server. Unlocks 6 spec.md files. Largest and most complex subsystem — requires MCP protocol implementation, server lifecycle management, tool/prompt/resource registration. XL-sized effort.

## Work Log\n\n- Proposal written at plan/proposals/typescript-mcp-server-commands.md

## Implementation Plan\n\n- [x] Add @modelcontextprotocol/sdk dependency\n- [x] Extract runScriptHandler from http-server.ts into shared utility (duplicated as runHandler)\n- [x] Create mcp-server.ts with all 6 MCP commands\n- [x] Register commands in register.ts\n- [x] Add MCP spec files to spec-runner.test.ts\n- [x] Run tests and fix failures (443 passed, 2 pre-existing failures, 13 skipped)

## Summary of Changes

All 6 MCP server commands ported to TypeScript: Mcp server, Mcp tool, Mcp tool call, Mcp resource, Mcp prompt, Stop mcp server. Uses low-level MCP SDK Server class with per-request server instances for Streamable HTTP transport (workaround for TS SDK limitations). All MCP spec tests pass. 443 total tests passing.
