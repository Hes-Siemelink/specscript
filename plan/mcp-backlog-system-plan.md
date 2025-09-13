# MCP Backlog Ticket Planning System - Technical Plan

## Executive Summary

This plan outlines the implementation of a comprehensive MCP Backlog ticket planning system within SpecScript. The system will provide CRUD operations for stories/tickets, state management, and backlog ordering while aligning MCP and HTTP server patterns for consistency and improving the overall server framework.

## Current Architecture Analysis

### MCP Server Implementation
- **Location**: `src/main/kotlin/specscript/commands/mcp/McpServer.kt`
- **Key Patterns**:
  - Server registry with `mutableMapOf<String, Server>()`
  - Tool registration with `server.addTool(toolName, tool, context.clone())`
  - Lifecycle management via `startServer()` and `stopServer()`
  - Spec structure: `name`, `version`, `tools`, `resources`, `prompts`

### HTTP Server Implementation  
- **Location**: `src/main/kotlin/specscript/commands/http/HttpServer.kt`
- **Key Patterns**:
  - Server registry with `mutableMapOf<Int, Javalin>()`
  - Endpoint registration with `server.addHandler(path, method, handler, context)`
  - Lifecycle management via `stop(port)`
  - Spec structure: `port`, `endpoints` with method handlers (`get`, `post`, etc.)

### Current Discrepancies
1. **Property Naming**: MCP uses `tools`/`resources`/`prompts` while HTTP uses `endpoints`
2. **Lifecycle**: MCP has explicit `stop: true` flag, HTTP requires separate stop call
3. **Context Handling**: Different variable injection patterns (`input` vs `request`)
4. **Server Identification**: MCP uses `name`, HTTP uses `port`

## Requirements Analysis

### Functional Requirements
1. **Ticket Management**:
   - Create, read, update, delete tickets/stories
   - Support for ticket properties: id, title, description, state, priority, assignee
   - State transitions: "todo" → "doing" → "done"
   - Backlog ordering and reordering capabilities

2. **Data Persistence**:
   - In-memory storage for prototype (extensible to external persistence)
   - JSON/YAML export/import capabilities
   - State preservation across server restarts

3. **MCP Tool Interface**:
   - `list_tickets` - Get all tickets with optional filtering
   - `get_ticket` - Get specific ticket by ID
   - `create_ticket` - Create new ticket
   - `update_ticket` - Update existing ticket
   - `delete_ticket` - Delete ticket
   - `move_ticket` - Change ticket state
   - `reorder_backlog` - Reorder tickets in backlog

### Technical Requirements
1. **Server Pattern Alignment**:
   - Standardize property names across MCP and HTTP servers
   - Implement consistent lifecycle management
   - Unified context variable handling

2. **Standalone Definitions**:
   - Create `Mcp tool`, `Http endpoint` commands for standalone definitions
   - Allow referencing servers by name/port
   - Enable modular tool/endpoint composition

3. **Testing Framework Integration**:
   - Server lifecycle management in tests
   - Parallel server support for test isolation
   - Automated cleanup after test completion

## Technical Design

### 1. MCP Backlog System Design

#### Data Models

```yaml
# Ticket data structure
Ticket:
  id: string (UUID)
  title: string
  description: string
  state: enum [todo, doing, done]
  priority: enum [low, medium, high]
  assignee: string (optional)
  created_at: timestamp
  updated_at: timestamp
  order: integer (for backlog ordering)
```

#### Tool Definitions

```yaml
# MCP Tools for ticket management
tools:
  list_tickets:
    description: List all tickets with optional filtering
    inputSchema:
      state:
        type: string
        enum: [todo, doing, done]
        description: Filter by ticket state
      assignee:
        type: string
        description: Filter by assignee
    
  get_ticket:
    description: Get specific ticket by ID
    inputSchema:
      id:
        type: string
        description: Ticket ID
        
  create_ticket:
    description: Create a new ticket
    inputSchema:
      title:
        type: string
        description: Ticket title
      description:
        type: string
        description: Ticket description
      priority:
        type: string
        enum: [low, medium, high]
        default: medium
      assignee:
        type: string
        description: Optional assignee
        
  update_ticket:
    description: Update existing ticket
    inputSchema:
      id:
        type: string
        description: Ticket ID
      title:
        type: string
        description: Updated title
      description:
        type: string
        description: Updated description
      priority:
        type: string
        enum: [low, medium, high]
      assignee:
        type: string
        
  delete_ticket:
    description: Delete a ticket
    inputSchema:
      id:
        type: string
        description: Ticket ID
        
  move_ticket:
    description: Move ticket to different state
    inputSchema:
      id:
        type: string
        description: Ticket ID
      state:
        type: string
        enum: [todo, doing, done]
        description: New state
        
  reorder_backlog:
    description: Reorder tickets in backlog
    inputSchema:
      ticket_ids:
        type: array
        items:
          type: string
        description: Ordered list of ticket IDs
```

#### Implementation Structure

```
src/main/kotlin/specscript/commands/mcp/
├── McpServer.kt (existing)
├── backlog/
│   ├── BacklogService.kt           # Core business logic
│   ├── TicketRepository.kt         # Data persistence layer
│   ├── TicketModel.kt             # Data models
│   └── BacklogTools.kt            # MCP tool implementations
```

### 2. Server Pattern Alignment

#### Unified Server Schema Properties

```yaml
# Common server properties
ServerBase:
  name: string              # Server identifier
  version: string           # Server version
  stop: boolean            # Lifecycle control
  
# MCP Server extension
McpServer extends ServerBase:
  tools: object            # MCP tools
  resources: object        # MCP resources  
  prompts: object          # MCP prompts
  
# HTTP Server extension  
HttpServer extends ServerBase:
  port: integer            # Required for HTTP
  endpoints: object        # HTTP endpoints
```

#### Lifecycle Management Alignment

```kotlin
// Common interface for server lifecycle
interface ServerLifecycle {
    fun start()
    fun stop()
    fun isRunning(): Boolean
}

// Update existing implementations
object McpServer : ServerLifecycle { ... }
object HttpServer : ServerLifecycle { ... }
```

### 3. Standalone Tool/Endpoint Definitions

#### New Commands

```yaml
# Standalone MCP tool definition
Mcp tool:
  server: my-server          # Reference to server name
  name: tool-name
  description: Tool description
  inputSchema: { ... }
  script: tool-script.cli

# Standalone HTTP endpoint definition  
Http endpoint:
  server: 2525              # Reference to server port
  path: /api/endpoint
  method: get
  script: endpoint-script.cli
```

#### Implementation

```kotlin
// New command handlers
object McpTool : CommandHandler("Mcp tool", "ai/mcp") {
    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val serverName = data.getParameter("server").textValue()
        val server = McpServer.getServer(serverName)
            ?: throw ScriptingException("MCP server '$serverName' not found")
        
        // Add tool to existing server
        server.addTool(data, context)
        return null
    }
}

object HttpEndpoint : CommandHandler("Http endpoint", "core/http") {
    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val port = data.getParameter("server").intValue()
        val server = HttpServer.getServer(port)
            ?: throw ScriptingException("HTTP server on port $port not found")
            
        // Add endpoint to existing server
        server.addEndpoint(data, context)
        return null
    }
}
```

### 4. Testing Framework Integration

#### Server Lifecycle Management

```kotlin
// Enhanced test support
object TestServerManager {
    private val mcpServers = mutableMapOf<String, Server>()
    private val httpServers = mutableMapOf<Int, Javalin>()
    
    fun startMcpServer(name: String, config: McpServerInfo): Server
    fun stopMcpServer(name: String)
    fun startHttpServer(port: Int): Javalin
    fun stopHttpServer(port: Int)
    fun stopAllServers() // Cleanup for tests
}
```

#### Test Specification Updates

```yaml
# Enhanced test lifecycle
Test case: MCP Backlog Integration
  setup:
    Mcp server:
      name: test-backlog
      version: "1.0.0"
      tools: { ... }
      
  test:
    # Test operations
    
  cleanup:
    Mcp server:
      name: test-backlog
      stop: true
```

## Implementation Strategy

### Phase 1: Server Pattern Alignment (2-3 days)
1. **Standardize Server Schemas**:
   - Update `McpServer.schema.yaml` to include common properties
   - Update `Http server.schema.yaml` to align with common pattern
   - Add `name` and `version` support to HTTP server

2. **Implement Lifecycle Interface**:
   - Create `ServerLifecycle` interface
   - Update both server implementations
   - Add server registry access methods

3. **Context Variable Alignment**:
   - Standardize variable injection patterns
   - Ensure consistent `input` and `request` handling
   - Update documentation

### Phase 2: Standalone Definitions (2-3 days)
1. **Create New Commands**:
   - Implement `Mcp tool` command handler
   - Implement `Http endpoint` command handler
   - Add schema definitions and documentation

2. **Server Registry Access**:
   - Add public methods to access running servers
   - Implement error handling for missing servers
   - Update existing commands to use registry

3. **Testing and Documentation**:
   - Create test cases for standalone definitions
   - Update specification documentation
   - Create usage examples

### Phase 3: MCP Backlog System (3-4 days)
1. **Core Data Layer**:
   - Implement `TicketModel` and related data structures
   - Create `TicketRepository` for data persistence
   - Add JSON/YAML serialization support

2. **Business Logic**:
   - Implement `BacklogService` with CRUD operations
   - Add state transition validation
   - Implement backlog ordering logic

3. **MCP Tool Integration**:
   - Create tool handlers for all backlog operations
   - Implement error handling and validation
   - Add comprehensive input schema definitions

### Phase 4: Testing and Integration (2-3 days)
1. **Test Framework Updates**:
   - Enhance test server lifecycle management
   - Add parallel server testing support
   - Implement automatic cleanup

2. **Integration Testing**:
   - Create comprehensive test suite for backlog system
   - Test server pattern alignment
   - Validate standalone definitions

3. **Documentation and Examples**:
   - Update AGENTS.md with new patterns
   - Create sample backlog implementations
   - Document migration guide for existing projects

## Migration Path

### Breaking Changes Assessment
1. **Schema Changes**: Minimal impact - new optional properties
2. **API Changes**: Backward compatible - existing code continues working
3. **Test Changes**: May require updates to leverage new lifecycle management

### Migration Steps
1. **Immediate**: Existing MCP and HTTP servers continue working unchanged
2. **Gradual**: Teams can adopt new patterns as needed
3. **Optional**: Standalone definitions provide additional flexibility
4. **Future**: Consider deprecating old patterns in favor of unified approach

## Testing Strategy

### Unit Tests
- `BacklogService` operations and state transitions
- `TicketRepository` data persistence and retrieval
- Server lifecycle management

### Integration Tests  
- End-to-end MCP tool operations
- Server pattern alignment validation
- Standalone definition functionality

### Performance Tests
- Concurrent ticket operations
- Server startup/shutdown performance
- Memory usage with large backlogs

## Risk Assessment

### Technical Risks
1. **Backward Compatibility**: Mitigated by maintaining existing APIs
2. **Server Conflicts**: Addressed by improved registry management
3. **State Management**: Handled by robust repository pattern

### Mitigation Strategies
1. **Gradual Rollout**: Phase-based implementation allows early feedback
2. **Comprehensive Testing**: Extensive test coverage prevents regressions
3. **Documentation**: Clear migration guides reduce adoption friction

## Success Criteria

1. **Functional**: Complete MCP backlog system with all CRUD operations
2. **Technical**: Aligned server patterns with consistent APIs
3. **Usability**: Standalone definitions enable modular composition
4. **Quality**: 90%+ test coverage and comprehensive documentation
5. **Performance**: Sub-100ms response times for typical operations

## Conclusion

This plan provides a comprehensive approach to implementing the MCP Backlog system while significantly improving SpecScript's server framework. The phased approach ensures minimal disruption while delivering substantial value through improved consistency, modularity, and functionality.

The alignment of MCP and HTTP server patterns creates a foundation for future server types, while standalone definitions enable more flexible and maintainable SpecScript applications. The backlog system serves as both a valuable feature and a demonstration of these improved patterns.