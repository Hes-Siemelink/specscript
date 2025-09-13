# MCP Backlog System - Focused Implementation Plan

## Executive Summary

This plan outlines the implementation of a focused MCP Backlog ticket management system within SpecScript. The scope is intentionally limited to the MCP Backlog functionality only, excluding server alignment work which will be addressed in a separate plan. This approach allows us to deliver value quickly while learning patterns that will inform future server architecture decisions.

## Scope Definition

### In Scope
1. **MCP Backlog System**: Complete ticket/story management with CRUD operations
2. **MCP Testing Strategy**: Manual lifecycle management for unit tests
3. **MCP-only Implementation**: Focus solely on MCP server patterns
4. **Learning Documentation**: Capture insights for future server alignment

### Out of Scope (Separate Plan)
1. **Server Alignment**: HTTP/MCP server pattern unification
2. **Standalone Definitions**: `Mcp tool`, `Http endpoint` commands
3. **Test Framework Integration**: Automatic server lifecycle in test infrastructure
4. **Property Standardization**: `name` property for HTTP server, `port` separation

## Current Architecture Analysis

### MCP Server Strengths
- **Clean Structure**: tools/resources/prompts organization
- **Registry Pattern**: `mutableMapOf<String, Server>()` with name-based access
- **Lifecycle Management**: Explicit start/stop with `server.close()`
- **Context Handling**: Consistent `INPUT_VARIABLE` pattern
- **Script Flexibility**: Support for both file and inline scripts

### Key Patterns to Leverage
```kotlin
// Server registry with name-based access
private val servers = mutableMapOf<String, Server>()

// Tool registration pattern
server.addTool(toolName, tool, context.clone())

// Lifecycle management
fun stopServer(name: String) {
    val server = servers.remove(name) ?: return
    runBlocking { server.close() }
}
```

## Requirements Analysis

### Functional Requirements
1. **Ticket Management**:
   - Create, read, update, delete tickets/stories
   - Properties: id, title, description, state, priority, assignee, timestamps
   - State transitions: "todo" → "doing" → "done"
   - Backlog ordering with reorder capability

2. **Data Persistence**:
   - In-memory storage (extensible design)
   - JSON/YAML export/import
   - State preservation patterns

3. **MCP Tool Interface**:
   - `list_tickets` - Get all tickets with filtering
   - `get_ticket` - Get specific ticket by ID
   - `create_ticket` - Create new ticket
   - `update_ticket` - Update existing ticket
   - `delete_ticket` - Delete ticket
   - `move_ticket` - Change ticket state
   - `reorder_backlog` - Reorder tickets

### Technical Requirements
1. **MCP Server Testing**:
   - Manual start/stop capability for unit tests
   - NOT automatic like HTTP server in test framework
   - Explicit lifecycle commands for testing scenarios

2. **YAML Correctness**:
   - Valid YAML structure in all examples
   - Proper nesting and SpecScript constraints
   - No duplicate keys, correct use of --- separators

## Technical Design

### 1. Data Models

```kotlin
data class Ticket(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val state: TicketState = TicketState.TODO,
    val priority: Priority = Priority.MEDIUM,
    val assignee: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val order: Int = 0
)

enum class TicketState { TODO, DOING, DONE }
enum class Priority { LOW, MEDIUM, HIGH }
```

### 2. MCP Tool Definitions

```yaml
---
Mcp server:
  name: backlog-server
  version: "1.0.0"

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
      script: |
        Get backlog tickets:
          state: "${input.state}"
          assignee: "${input.assignee}"

    get_ticket:
      description: Get specific ticket by ID
      inputSchema:
        id:
          type: string
          description: Ticket ID
      script: |
        Get backlog ticket:
          id: "${input.id}"

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
      script: |
        Create backlog ticket:
          title: "${input.title}"
          description: "${input.description}"
          priority: "${input.priority}"
          assignee: "${input.assignee}"

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
      script: |
        Update backlog ticket:
          id: "${input.id}"
          title: "${input.title}"
          description: "${input.description}"
          priority: "${input.priority}"
          assignee: "${input.assignee}"

    delete_ticket:
      description: Delete a ticket
      inputSchema:
        id:
          type: string
          description: Ticket ID
      script: |
        Delete backlog ticket:
          id: "${input.id}"

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
      script: |
        Move backlog ticket:
          id: "${input.id}"
          state: "${input.state}"

    reorder_backlog:
      description: Reorder tickets in backlog
      inputSchema:
        ticket_ids:
          type: array
          items:
            type: string
          description: Ordered list of ticket IDs
      script: |
        Reorder backlog:
          ticket_ids: "${input.ticket_ids}"
```

### 3. Implementation Structure

```
src/main/kotlin/specscript/commands/mcp/
├── McpServer.kt (existing)
├── backlog/
│   ├── BacklogService.kt           # Core business logic
│   ├── TicketRepository.kt         # Data persistence layer
│   ├── TicketModel.kt             # Data models
│   └── BacklogCommands.kt         # Command handlers
```

### 4. MCP Server Testing Strategy

Unlike HTTP servers in the test framework, MCP servers will require explicit lifecycle management:

```yaml
---
# Test with manual MCP server lifecycle
Test case: Create and retrieve ticket
  setup:
    Start mcp server:
      name: test-backlog
      version: "1.0.0"
      tools:
        create_ticket:
          description: Create a new ticket
          inputSchema:
            title: {type: string}
            description: {type: string}
          script: |
            Create backlog ticket:
              title: "${input.title}"
              description: "${input.description}"
  
  test:
    # Test operations here
    Create backlog ticket:
      title: "Test ticket"
      description: "Test description"
      
  cleanup:
    Stop mcp server:
      name: test-backlog
```

### 5. Command Handlers

```kotlin
object CreateBacklogTicket : CommandHandler("Create backlog ticket", "ai/mcp/backlog") {
    override fun execute(data: ObjectNode, context: ScriptContext): JsonNode? {
        val service = BacklogService.getInstance()
        val ticket = service.createTicket(
            title = data.getParameter("title").textValue(),
            description = data.getParameter("description").textValue(),
            priority = Priority.valueOf(data.getParameter("priority")?.textValue()?.uppercase() ?: "MEDIUM"),
            assignee = data.getParameter("assignee")?.textValue()
        )
        return Json.toJsonNode(ticket)
    }
}

// Similar pattern for other operations...
```

## Implementation Strategy

### Phase 1: Core Data Layer (2 days)
1. **Data Models**:
   - Implement `Ticket`, `TicketState`, `Priority` data classes
   - Add JSON serialization support
   - Create proper validation

2. **Repository Layer**:
   - Implement `TicketRepository` with in-memory storage
   - Add CRUD operations with proper error handling
   - Implement ordering and state transition logic

3. **Service Layer**:
   - Create `BacklogService` with business logic
   - Add state transition validation
   - Implement backlog ordering algorithms

### Phase 2: MCP Integration (2 days)
1. **Command Handlers**:
   - Implement all backlog command handlers
   - Add proper input validation and error handling
   - Ensure consistent JSON output format

2. **MCP Tool Configuration**:
   - Create sample backlog server configurations
   - Test all tool operations manually
   - Validate input schemas and error responses

3. **Documentation**:
   - Create usage examples and patterns
   - Document YAML structure requirements
   - Add troubleshooting guide

### Phase 3: Testing and Validation (2 days)
1. **Unit Tests**:
   - Test repository operations and data integrity
   - Test service layer business logic
   - Test command handlers with various inputs

2. **Integration Tests**:
   - Test MCP server lifecycle with backlog tools
   - Validate end-to-end ticket operations
   - Test error scenarios and edge cases

3. **Manual Testing Strategy**:
   - Document explicit MCP server start/stop for tests
   - Create test scenarios with proper cleanup
   - Validate against existing MCP patterns

## Testing Strategy

### Unit Tests
- `BacklogService` CRUD operations and state transitions
- `TicketRepository` data persistence and ordering
- Command handler input validation and error handling

### Integration Tests
- End-to-end MCP tool operations
- Server lifecycle management in test scenarios
- Proper cleanup and resource management

### Manual Testing Pattern
```yaml
---
# Manual MCP server testing approach
Test setup: Start backlog MCP server
Test execution: Execute backlog operations
Test cleanup: Stop MCP server explicitly
```

### YAML Validation
- All YAML examples must be valid and parseable
- Proper indentation and structure
- Correct use of SpecScript constraints

## Learning Documentation

### Insights to Capture
1. **MCP Server Patterns**: What works well in current implementation
2. **Testing Challenges**: Manual vs automatic lifecycle management
3. **Data Persistence**: In-memory vs external storage patterns
4. **Error Handling**: MCP-specific error response patterns

### Future Server Alignment Inputs
1. **Registry Patterns**: Name-based vs port-based server access
2. **Lifecycle Management**: Consistent start/stop patterns
3. **Context Handling**: Variable injection consistency
4. **Testing Integration**: Automatic vs manual server lifecycle

## Risk Assessment

### Technical Risks
1. **MCP Server Stability**: Mitigated by following existing patterns
2. **Data Consistency**: Addressed by proper repository design
3. **Test Isolation**: Handled by explicit server lifecycle management

### Mitigation Strategies
1. **Follow Existing Patterns**: Leverage proven MCP server implementation
2. **Explicit Testing**: Manual server lifecycle provides better control
3. **Documentation**: Clear examples and troubleshooting guides

## Success Criteria

1. **Functional**: Complete MCP backlog system with all CRUD operations
2. **Testing**: Manual MCP server testing capability for unit tests
3. **Documentation**: Clear usage patterns and examples
4. **Learning**: Documented insights for future server alignment
5. **Quality**: 90%+ test coverage with proper error handling
6. **Performance**: Sub-100ms response times for typical operations

## Future Work (Separate Plan)

The following items are explicitly excluded from this plan and will be addressed in the "Server Alignment Plan":

1. **Server Pattern Unification**: Aligning HTTP and MCP server patterns
2. **Standalone Definitions**: `Mcp tool` and `Http endpoint` commands
3. **Property Standardization**: Adding `name` to HTTP server, separating `port`
4. **Test Framework Integration**: Automatic server lifecycle in test infrastructure
5. **Context Variable Alignment**: Standardizing `input` vs `request` patterns

## Conclusion

This focused plan delivers a complete MCP Backlog system while maintaining scope discipline. By excluding server alignment work, we can deliver value quickly and learn patterns that will inform future architectural decisions. The manual testing approach provides better control and explicit lifecycle management, which is appropriate for the current MCP server implementation.

The insights gained from this implementation will directly inform the separate Server Alignment Plan, ensuring that future unification work is based on practical experience rather than theoretical design.