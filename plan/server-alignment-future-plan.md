# Server Alignment Plan - Future Architecture Work

## Executive Summary

This plan outlines the future work for aligning MCP and HTTP server patterns within SpecScript. This work is explicitly separated from the MCP Backlog implementation to allow the backlog system to be delivered quickly while learning patterns that will inform this alignment work. The insights gained from the MCP Backlog implementation will directly influence the approaches outlined here.

## Context and Dependencies

### Prerequisites
1. **MCP Backlog System Completed**: This plan depends on learnings from the focused MCP Backlog implementation
2. **Pattern Analysis**: Understanding of what works well vs what needs improvement in current server implementations
3. **Testing Strategy Validation**: Confirmation of manual vs automatic server lifecycle approaches

### Learning Inputs from MCP Backlog
The following insights from the MCP Backlog implementation will inform this plan:
- Registry pattern effectiveness (name-based vs port-based)
- Lifecycle management approaches and testing implications
- Context variable handling patterns
- Error handling and validation patterns
- Performance characteristics and resource management

## Current State Analysis

### MCP Server Implementation
**Strengths**:
- Clean tools/resources/prompts organization
- Name-based server registry: `mutableMapOf<String, Server>()`
- Explicit lifecycle with `stopServer(name: String)`
- Consistent context handling with `INPUT_VARIABLE`

**Areas for Improvement**:
- Manual testing lifecycle (could benefit from optional automation)
- Limited standalone tool composition

### HTTP Server Implementation  
**Strengths**:
- Port-based server registry: `mutableMapOf<Int, Javalin>()`
- Flexible endpoint registration
- Automatic test framework integration
- Rich context variables (`request` object)

**Areas for Improvement**:
- No `name` property for server identification
- `port` combined with other properties (should be separate)
- Different context variable patterns vs MCP

### Current Discrepancies
1. **Server Identification**: MCP uses `name`, HTTP uses `port` only
2. **Registry Patterns**: Different key types (String vs Int)
3. **Context Variables**: `INPUT_VARIABLE` vs `REQUEST_VARIABLE`
4. **Lifecycle Management**: Explicit vs implicit patterns
5. **Test Integration**: Manual vs automatic lifecycle

## Technical Design

### 1. Unified Server Schema Properties

```yaml
# Base server properties (applies to both MCP and HTTP)
ServerBase:
  name: string              # Universal server identifier
  version: string           # Server version
  stop: boolean            # Lifecycle control

# MCP Server (current structure maintained)
McpServer extends ServerBase:
  tools: object            # MCP tools
  resources: object        # MCP resources  
  prompts: object          # MCP prompts

# HTTP Server (enhanced with name, port separation)
HttpServer extends ServerBase:
  port: integer            # Network port (separate from name)
  endpoints: object        # HTTP endpoints
```

### 2. Registry Pattern Unification

```kotlin
// Unified server registry approach
interface ServerRegistry<T> {
    fun register(name: String, server: T)
    fun unregister(name: String): T?
    fun get(name: String): T?
    fun getByPort(port: Int): T? // For HTTP compatibility
}

object McpServerRegistry : ServerRegistry<Server> {
    private val servers = mutableMapOf<String, Server>()
    // Implementation...
}

object HttpServerRegistry : ServerRegistry<Javalin> {
    private val servers = mutableMapOf<String, Javalin>()
    private val portMapping = mutableMapOf<Int, String>() // Port -> Name mapping
    // Implementation...
}
```

### 3. Lifecycle Management Interface

```kotlin
interface ServerLifecycle {
    fun start()
    fun stop()
    fun isRunning(): Boolean
    fun getName(): String
}

// Enhanced server implementations
object McpServer : ServerLifecycle {
    fun startServer(name: String, config: McpServerInfo): Server
    fun stopServer(name: String)
    override fun getName(): String = "MCP Server"
}

object HttpServer : ServerLifecycle {
    fun startServer(name: String, port: Int, config: HttpServerInfo): Javalin
    fun stopServer(name: String) // Changed from port-based to name-based
    override fun getName(): String = "HTTP Server"
}
```

### 4. Context Variable Alignment

```kotlin
// Unified context variable patterns
const val INPUT_VARIABLE = "input"      // MCP and HTTP input data
const val REQUEST_VARIABLE = "request"  // HTTP-specific request context
const val CONTEXT_VARIABLE = "context"  // Shared execution context

// Standardized context population
interface ContextHandler {
    fun populateInputVariable(context: ScriptContext, data: JsonNode)
    fun populateRequestVariable(context: ScriptContext, request: Any?)
}
```

### 5. Standalone Server Definitions

```yaml
---
# Standalone MCP tool definition
Mcp tool:
  server: my-backlog-server    # Reference to server name
  name: create_ticket
  description: Create a new ticket
  inputSchema:
    title: {type: string}
    description: {type: string}
  script: create-ticket.cli

---
# Standalone HTTP endpoint definition  
Http endpoint:
  server: my-api-server       # Reference to server name (not port)
  path: /api/tickets
  method: post
  script: create-ticket-endpoint.cli
```

### 6. Enhanced Test Framework Integration

```kotlin
// Optional automatic server lifecycle for tests
object TestServerManager {
    fun enableAutoLifecycle(serverType: String, enabled: Boolean)
    fun startMcpServer(name: String, config: McpServerInfo): Server
    fun startHttpServer(name: String, port: Int, config: HttpServerInfo): Javalin
    fun stopServer(name: String)
    fun stopAllServers() // Test cleanup
}

// Test specification with optional automatic lifecycle
```

```yaml
---
# Flexible test server lifecycle
Test case: Server alignment validation
  setup:
    # Option 1: Automatic lifecycle (new capability)
    Http server:
      name: test-api
      port: 8080
      auto_lifecycle: true    # New property
      endpoints: { ... }
      
    # Option 2: Manual lifecycle (current MCP approach)
    Mcp server:
      name: test-mcp
      version: "1.0.0"
      tools: { ... }
      
  test:
    # Test operations reference servers by name
    Http post:
      server: test-api        # Reference by name, not port
      path: /test
      
  cleanup:
    # Automatic cleanup for auto_lifecycle servers
    # Manual cleanup for manual servers
    Mcp server:
      name: test-mcp
      stop: true
```

## Implementation Strategy

### Phase 1: Property Standardization (2-3 days)
**Prerequisites**: MCP Backlog implementation insights

1. **HTTP Server Enhancement**:
   - Add `name` property to HTTP server configuration
   - Separate `port` as distinct property (not part of identification)
   - Update existing HTTP samples to use new schema

2. **Registry Updates**:
   - Implement name-based HTTP server registry
   - Maintain port mapping for backward compatibility
   - Add server lookup methods by name

3. **Schema Migration**:
   - Update `Http server.schema.yaml` with new properties
   - Provide migration guide for existing projects
   - Ensure backward compatibility during transition

### Phase 2: Context Variable Alignment (2-3 days)
**Prerequisites**: Validation of context patterns from MCP Backlog

1. **Standardize Variable Names**:
   - Ensure consistent `INPUT_VARIABLE` usage across both servers
   - Standardize `REQUEST_VARIABLE` for HTTP-specific context
   - Add shared `CONTEXT_VARIABLE` for execution metadata

2. **Context Handler Implementation**:
   - Create unified context population interfaces
   - Update both server implementations to use standard patterns
   - Validate context variable availability in scripts

3. **Documentation Updates**:
   - Update AGENTS.md with standardized context patterns
   - Create migration guide for scripts using old patterns
   - Add examples of unified context usage

### Phase 3: Standalone Definitions (3-4 days)
**Prerequisites**: Confirmed server registry patterns work well

1. **Create New Commands**:
   - Implement `Mcp tool` command handler
   - Implement `Http endpoint` command handler
   - Add comprehensive input validation

2. **Server Registry Integration**:
   - Add public methods to access running servers by name
   - Implement error handling for missing servers
   - Update existing commands to leverage registry

3. **Testing and Examples**:
   - Create test cases for standalone definitions
   - Build sample applications using modular composition
   - Document best practices and patterns

### Phase 4: Enhanced Test Framework Integration (2-3 days)
**Prerequisites**: Learning from manual MCP testing approach

1. **Optional Automatic Lifecycle**:
   - Add `auto_lifecycle` property to server configurations
   - Implement automatic startup/shutdown for test framework
   - Maintain manual lifecycle option for explicit control

2. **Unified Test Server Management**:
   - Create `TestServerManager` for cross-server lifecycle
   - Implement parallel server support for test isolation
   - Add comprehensive cleanup mechanisms

3. **Test Framework Enhancement**:
   - Update test specification to support server references by name
   - Add automatic cleanup for auto-lifecycle servers
   - Validate mixed manual/automatic lifecycle scenarios

## Migration Strategy

### Backward Compatibility Approach
1. **Gradual Migration**: All existing code continues to work unchanged
2. **Opt-in Enhancements**: Teams adopt new patterns as needed
3. **Deprecation Timeline**: 6+ months before considering removal of old patterns

### Migration Steps
1. **Immediate (Phase 1)**: HTTP servers gain `name` property, maintain port-based access
2. **Gradual (Phase 2-3)**: New context patterns and standalone definitions available
3. **Optional (Phase 4)**: Enhanced test framework capabilities
4. **Long-term**: Consider deprecating port-only HTTP server access

### Breaking Changes Assessment
- **Minimal Impact**: New properties are optional and additive
- **Backward Compatible**: Existing APIs continue functioning
- **Migration Support**: Clear guides and automated migration tools where possible

## Learning Integration

### Insights from MCP Backlog Implementation
The following areas will be validated and refined based on MCP Backlog learnings:

1. **Registry Effectiveness**: How well does name-based registry work in practice?
2. **Lifecycle Management**: What are the real-world benefits/drawbacks of manual vs automatic?
3. **Context Patterns**: Which context variable approaches work best for different scenarios?
4. **Testing Approaches**: What testing patterns emerge as most effective?
5. **Performance Impact**: Resource usage and startup/shutdown performance characteristics

### Adaptation Strategy
- **Monitor MCP Backlog Implementation**: Gather real usage data and feedback
- **Iterate Design**: Refine alignment approach based on practical experience
- **Document Lessons**: Update this plan with insights and course corrections
- **Validate Assumptions**: Test alignment assumptions against real-world usage

## Risk Assessment

### Technical Risks
1. **Complexity Increase**: Mitigated by gradual migration and optional adoption
2. **Performance Impact**: Addressed by maintaining existing fast paths
3. **Breaking Changes**: Prevented by strict backward compatibility requirements

### Implementation Risks
1. **Scope Creep**: Mitigated by clear phase boundaries and learning checkpoints
2. **User Adoption**: Addressed by clear migration guides and optional adoption
3. **Testing Complexity**: Handled by maintaining existing test patterns alongside new ones

## Success Criteria

1. **Functional**: Unified server patterns with consistent APIs
2. **Compatibility**: 100% backward compatibility with existing projects
3. **Usability**: Standalone definitions enable modular composition
4. **Performance**: No degradation in startup time or resource usage
5. **Testing**: Enhanced test framework with optional automatic lifecycle
6. **Documentation**: Comprehensive migration guides and best practices
7. **Adoption**: Clear value proposition for teams to adopt new patterns

## Future Considerations

### Beyond Server Alignment
Once server patterns are unified, consider:
1. **Additional Server Types**: Database servers, message queue servers, etc.
2. **Server Composition**: Multi-server applications with unified management
3. **Configuration Management**: Environment-specific server configurations
4. **Monitoring and Observability**: Unified logging and metrics across server types

### Long-term Architecture
- **Server Framework**: SpecScript as a server application platform
- **Plugin Architecture**: Third-party server type extensions
- **Cloud Integration**: Native cloud platform server deployment
- **Performance Optimization**: Native compilation and resource optimization

## Conclusion

This Server Alignment Plan provides a comprehensive roadmap for unifying MCP and HTTP server patterns while maintaining backward compatibility and learning from practical MCP Backlog implementation experience. The phased approach ensures minimal disruption while delivering substantial value through improved consistency, modularity, and testing capabilities.

The plan explicitly builds on insights from the focused MCP Backlog implementation, ensuring that alignment decisions are informed by practical experience rather than theoretical design. This approach reduces risk and increases the likelihood of delivering server patterns that work well in real-world scenarios.