# MCP Client Implementation Plan

## Executive Summary

This plan outlines the implementation of MCP (Model Context Protocol) client functionality in SpecScript to enable
testing of MCP servers and provide developer utility for consuming MCP APIs. The client implementation will complement
the existing MCP server commands and follow established patterns in the codebase.

## Project Goals

1. **Testing Capability**: Enable comprehensive testing of MCP server functionality by providing client commands to
   interact with running servers
2. **Developer Utility**: Offer useful MCP client tools for consuming MCP APIs in a declarative, SpecScript-native way
3. **Pattern Consistency**: Align with existing HTTP client/server patterns while avoiding naming confusion with MCP
   server commands

## Current State Analysis

### Existing MCP Server Commands

- `Mcp server`: Server configuration and startup
- `Mcp tool`: Modular tool definitions
- `Mcp resource`: Modular resource definitions
- `Mcp prompt`: Modular prompt definitions

### Existing HTTP Client/Server Pattern

- **Server**: Single `Http server` command for configuration
- **Client**: Individual action commands (`GET`, `POST`, `PUT`, `DELETE`, `PATCH`)
- **Configuration**: Shared `Http request defaults` for common parameters

### MCP Kotlin SDK Client Capabilities

- `Client` class for server connection management
- `StdioClientTransport` for stdio connections
- Core methods: `listResources()`, `readResource()`
- Extensible for tool calling and prompt execution

## Three Naming Approaches

### Approach 1: Action-Based Commands (Recommended)

**Pattern**: Individual commands for each MCP action, following HTTP client model

**Commands**:

- `Call tool`: Execute MCP server tools
- `Read resource`: Access MCP server resources
- `Get prompt`: Retrieve and execute MCP server prompts
- `List tools`: Discover available server tools
- `List resources`: Discover available server resources
- `List prompts`: Discover available server prompts
- `Connect mcp`: Establish MCP server connection

**Pros**:

- Clear, action-oriented naming
- Follows established HTTP client pattern
- Each command has single responsibility
- Natural discoverability

**Cons**:

- More commands to implement
- Requires connection management across commands

### Approach 2: Unified Client Command

**Pattern**: Single `Mcp client` command with action sub-properties

**Commands**:

- `Mcp client`: Unified command with `action` property (call_tool, read_resource, get_prompt)

**Example**:

```yaml
Mcp client:
  server: my-server
  action: call_tool
  tool: greeting
  arguments:
    name: Alice
```

**Pros**:

- Single command to maintain
- Centralized connection management
- Consistent with `Mcp server` naming

**Cons**:

- Less discoverable actions
- Violates single responsibility principle
- More complex command structure

### Approach 3: Renamed Server + Action Commands

**Pattern**: Rename server commands to avoid confusion, use action commands for client

**Server Commands** (renamed):

- `Start mcp server` (was `Mcp server`)
- `Mcp tool definition` (was `Mcp tool`)
- `Mcp resource definition` (was `Mcp resource`)
- `Mcp prompt definition` (was `Mcp prompt`)

**Client Commands**:

- `Call Mcp tool`
- `Call mcp resource`
- `Call mcp prompt`
- `Connect to` -- use existing connection logic like it works now for HTTP

For the connection logic to work, you can at first define the needed connection properties on the commands themselves,
and later refactor to a shared `Mcp connection defaults` command that can be used by the `Connect to` command. No need
to go into that in the first iteration.

**Pros**:

- Crystal clear server/client distinction
- Action-oriented client commands
- Eliminates naming confusion

**Cons**:

- Breaking change for existing server commands
- More verbose command names
- Requires migration of existing code

## Recommended Approach: Action-Based Commands

**Approach 1** is recommended because:

- Follows proven HTTP client/server pattern
- Maintains backward compatibility
- Provides clear, discoverable actions
- Enables comprehensive MCP testing capabilities

## Technical Architecture

### Core Components

#### 1. Connection Management

```kotlin
object McpClientRegistry {
    private val clients = mutableMapOf<String, Client>()

    fun connect(serverName: String, transport: ClientTransport): Client
    fun getClient(serverName: String): Client
    fun disconnect(serverName: String)
}
```

#### 2. Transport Abstraction

- **StdioTransport**: For connecting to local MCP servers via stdio
- **ProcessTransport**: For spawning and connecting to MCP server processes
- **NetworkTransport**: Future support for network-based MCP servers

#### 3. Command Handlers

```kotlin
object CallTool : CommandHandler("Call tool", "ai/mcp"), ObjectHandler
object ReadResource : CommandHandler("Read resource", "ai/mcp"), ObjectHandler
object GetPrompt : CommandHandler("Get prompt", "ai/mcp"), ObjectHandler
object ConnectMcp : CommandHandler("Connect mcp", "ai/mcp"), ObjectHandler
```

### Integration Strategy

#### Server-Client Integration

```yaml specscript
# Start server
Mcp server:
  name: test-server
  tools:
    greet:
      description: Say hello
      script:
        Output: Hello ${input.name}!

# Connect client
Connect mcp:
  server: test-server
  transport: stdio

# Call tool
Call tool:
  server: test-server
  tool: greet
  arguments:
    name: Alice

Expected output: Hello Alice!
```

#### Standalone Client Usage

```yaml specscript
# Connect to external server
Connect mcp:
  server: external-server
  transport:
    type: process
    command: [ "python", "my-mcp-server.py" ]

# Use the server
Call tool:
  server: external-server
  tool: analyze_code
  arguments:
    code: "def hello(): pass"
```

## Implementation Phases

### Phase 1: Core Client Infrastructure

**Duration**: 1-2 weeks

**Deliverables**:

- `Connect mcp` command with stdio transport
- Connection registry and lifecycle management
- Basic error handling and connection validation
- Integration tests with existing MCP servers

**Success Criteria**:

- Can connect to locally started MCP servers
- Connection lifecycle properly managed
- Error handling for connection failures

### Phase 2: Tool Interaction Commands

**Duration**: 1-2 weeks

**Deliverables**:

- `Call tool` command implementation
- `List tools` command implementation
- Tool argument validation and mapping
- Comprehensive test coverage

**Success Criteria**:

- Can discover and call MCP server tools
- Proper argument passing and result handling
- Tool execution errors handled gracefully

### Phase 3: Resource and Prompt Commands

**Duration**: 1-2 weeks

**Deliverables**:

- `Read resource` command implementation
- `Get prompt` command implementation
- `List resources` and `List prompts` commands
- Documentation and examples

**Success Criteria**:

- Full MCP resource and prompt interaction capability
- Complete specification documentation
- Working examples for all use cases

### Phase 4: Advanced Transport Support

**Duration**: 1-2 weeks

**Deliverables**:

- Process transport for external MCP servers
- Network transport foundation
- Configuration management improvements
- Performance optimizations

**Success Criteria**:

- Can connect to external MCP server processes
- Robust transport layer abstraction
- Good performance characteristics

## Testing Strategy

### Unit Testing

- Command handler logic
- Connection management
- Transport layer functionality
- Error handling scenarios

### Integration Testing

- Server-client roundtrip testing
- Multi-server connection scenarios
- Transport switching scenarios
- Resource cleanup testing

### Specification Testing

- All commands documented with executable examples
- End-to-end scenarios in specification files
- Performance and reliability testing

### Test Infrastructure

```yaml specscript
Test case: MCP tool roundtrip
  setup:
    # Start test server
    Mcp server:
      name: test-server
      tools:
        echo:
          description: Echo input
          script: Output: ${input.message}

    # Connect client
    Connect mcp:
      server: test-server

  test:
    Call tool:
      server: test-server
      tool: echo
      arguments:
        message: "Hello World"

    Expected output: Hello World

  cleanup:
    # Cleanup handled automatically
```

## Risk Assessment

### Technical Risks

**Risk**: MCP Kotlin SDK client API limitations

- **Likelihood**: Medium
- **Impact**: High
- **Mitigation**: Early prototyping with SDK, fallback to protocol implementation

**Risk**: Connection lifecycle complexity

- **Likelihood**: Medium
- **Impact**: Medium
- **Mitigation**: Robust testing, clear connection patterns

**Risk**: Transport layer abstraction challenges

- **Likelihood**: Low
- **Impact**: Medium
- **Mitigation**: Start with stdio, iterate to other transports

### Project Risks

**Risk**: Scope creep beyond core functionality

- **Likelihood**: Medium
- **Impact**: Medium
- **Mitigation**: Clear phase boundaries, focus on testing use case first

**Risk**: Naming confusion with server commands

- **Likelihood**: Low
- **Impact**: Low
- **Mitigation**: Clear documentation, distinct action-based naming

## Success Criteria

### Functional Success

- [ ] Can connect to MCP servers via multiple transport mechanisms
- [ ] Can discover and interact with all MCP server capabilities (tools, resources, prompts)
- [ ] Provides comprehensive testing capability for MCP server implementations
- [ ] Integrates seamlessly with existing SpecScript patterns

### Quality Success

- [ ] All commands have complete specification documentation
- [ ] Test coverage > 90% for all client functionality
- [ ] Performance meets or exceeds HTTP client patterns
- [ ] Error handling provides clear, actionable feedback

### Adoption Success

- [ ] MCP server tests utilize client commands for validation
- [ ] Documentation includes realistic developer use cases
- [ ] Community feedback validates utility for MCP development workflows

## Next Steps

1. **Create detailed specifications** for each client command
2. **Implement Phase 1** core infrastructure
3. **Validate approach** with existing MCP server implementations
4. **Iterate based on feedback** from testing and developer usage
5. **Expand transport support** as needed

## Conclusion

This plan provides a comprehensive approach to MCP client implementation that balances testing utility, developer
experience, and architectural consistency. The action-based command approach aligns with established patterns while
providing the flexibility needed for comprehensive MCP server testing and developer utility.

The phased implementation approach minimizes risk while delivering value incrementally, and the focus on
specification-driven development ensures the implementation remains aligned with SpecScript's core principles.