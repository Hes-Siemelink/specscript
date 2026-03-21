---
name: specscript-servers
description: MCP and HTTP server architecture patterns in SpecScript. Registry design, lifecycle management, testing strategy, YAML correctness. Use when working on server commands (MCP server, HTTP server) or their specifications.
compatibility: Requires the specscript project structure.
metadata:
  author: specscript
  version: "1.0"
---

## Overview

SpecScript implements server patterns for both MCP and HTTP servers with similar but not identical structures.
This skill covers the implementation details and patterns for both.

## Common server patterns

- Server registries using `mutableMapOf` for tracking running servers
- Lifecycle management with start/stop operations
- Context cloning for isolated script execution
- Support for both inline scripts and external file references

## MCP server specifics

- Registry keyed by server `name` (string)
- Schema: `name`, `version`, `tools`, `resources`, `prompts`, `stop`
- Uses MCP Kotlin SDK with stdio transport
- Lifecycle: explicit `stop: true` flag for shutdown

### Critical MCP patterns

- Use name-based server registry: `servers = mutableMapOf<String, Server>()`
- Follow tools/resources/prompts structure (not endpoints like HTTP)
- Implement explicit lifecycle: `stopServer(name: String)` with `server.close()`
- Use `INPUT_VARIABLE` for consistent context handling
- Support both inline scripts and external file references

### MCP testing strategy

- MCP servers require **manual lifecycle management** in tests (not automatic like HTTP)
- Use explicit start/stop commands: `Start mcp server` / `Stop mcp server`
- NOT integrated into test framework automatic lifecycle
- This provides better control and explicit resource management

### MCP backlog system patterns

- Follow `create_ticket`, `update_ticket`, `delete_ticket`, `list_tickets` naming
- Use standard CRUD operations with proper state management
- Implement ticket states: `todo`, `doing`, `done` with validation
- Support filtering by state, assignee in list operations
- Include proper error handling for missing tickets/invalid states

### YAML correctness for MCP

- All MCP tool examples must be valid YAML with proper nesting
- Use `---` separators between multiple commands (YAML constraint)
- Avoid duplicate keys — use list syntax for repetition
- Proper indentation for inputSchema properties
- Schema validation: `type`, `enum`, `description` properties must be correctly structured

## HTTP server specifics

- Registry keyed by `port` (integer)
- Schema: `port`, `endpoints` with method handlers
- Uses Ktor with Netty for HTTP handling
- Lifecycle: separate stop command by port

## Server alignment opportunities

- Property naming consistency (both should support `name`, `version`, `stop`)
- Unified lifecycle interface implementation
- Consistent context variable handling (`input` vs `request`)
- Server registry access patterns for modular tool/endpoint definitions
