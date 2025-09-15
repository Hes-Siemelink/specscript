# MCP Client Transport Implementation Plan

## Executive Summary

This plan outlines the implementation of proper MCP client transport mechanisms in SpecScript, addressing the challenges
of stdio communication when client and server run in the same process, and expanding to support multiple transport types
including process-based stdio and streamable HTTP.

## Current State Analysis

### What We Have (In-Process Communication)

- ✅ **Direct method calls** using Kotlin reflection to invoke server's `handleCallTool`
- ✅ **Proper MCP protocol structures** (CallToolRequest, CallToolResult)
- ✅ **Real tool execution** with actual SpecScript execution
- ✅ **Works for testing** - validates MCP server functionality within same JVM

### Limitations of Current Approach

- ❌ **Not true MCP protocol** - bypasses JSON-RPC transport layer
- ❌ **Cannot connect to external servers** - only works with in-process servers
- ❌ **Stdio conflict** - both client and server compete for stdin/stdout in same process
- ❌ **No session management** - each call is independent

## Transport Architecture Overview

### Transport Types to Support

1. **Internal** (current) - for testing and internal server validation
2. **Stdio** - launch external processes via shell command, including SpecScript files
3. **HTTP** - connect to HTTP-based MCP servers

### Transport Selection Strategy

```yaml specscript
Call Mcp tool:
  server: my-server
  transport:
    # Option 1: Internal (testing/validation)
    type: internal

    # Option 2: Stdio - launch via shell command
    type: stdio
    command: cli my-mcp-server.spec.md     # SpecScript server file
    # OR
    command: python external-server.py    # External MCP server

    # Option 3: HTTP
    type: http
    url: "http://localhost:3000/mcp"

  tool: my-tool
  arguments: { ... }
```

## Implementation Phases

### Phase 1: Transport Abstraction Layer

**Duration**: 1-2 weeks

**Deliverables**:

- `McpClientTransport` interface abstraction
- `InProcessTransport` (current implementation, cleaned up)
- Transport registry and lifecycle management
- Connection pooling for session-based transports

**Technical Design**:

```kotlin
interface McpClientTransport {
    suspend fun connect(): Boolean
    suspend fun callTool(request: CallToolRequest): CallToolResult
    suspend fun listTools(): ListToolsResult
    suspend fun close()
}

class InternalTransport(val server: Server) : McpClientTransport
class StdioTransport(val command: String) : McpClientTransport  // Shell command as string
class HttpTransport(val baseUrl: String) : McpClientTransport
```

**Success Criteria**:

- Existing in-process functionality works through abstraction
- Clear interface for adding new transport types
- Proper resource cleanup and error handling

### Phase 2: Stdio Transport

**Duration**: 1-2 weeks

**Deliverables**:

- Shell command execution with stdio pipe management
- Long-lived session support with connection pooling
- Graceful process startup/shutdown

**Implementation**:

```kotlin
class StdioTransport(
    private val command: String  // Shell command to execute
) : McpClientTransport {

    private var process: Process? = null
    private var client: Client? = null

    suspend fun connect(): Boolean {
        // Execute shell command - let shell handle parsing and execution
        process = ProcessBuilder("sh", "-c", command).start()

        // Create MCP client with stdio transport
        client = Client(clientInfo = Implementation("specscript-client", "1.0.0"))
        val transport = StdioClientTransport(
            input = process.inputStream.asSource().buffered(),
            output = process.outputStream.asSink().buffered()
        )

        client.connect(transport)
        return true
    }

    override suspend fun close() {
        client?.close()
        process?.destroy()
    }
}
```

**Usage Examples**:

```yaml specscript
# Connect to SpecScript MCP server file
Call Mcp tool:
  transport:
    type: stdio
    command: cli my-mcp-server.spec.md
  tool: greet
  arguments:
    name: Alice

# Connect to Python MCP server
Call Mcp tool:
  transport:
    type: stdio
    command: python /path/to/mcp-server.py
  tool: analyze_code
  arguments:
    code: "def hello(): pass"

# Connect to Node.js MCP server with args
Call Mcp tool:
  transport:
    type: stdio
    command: node /path/to/server.js --verbose
  tool: filesystem_read
  arguments:
    path: "/tmp/file.txt"
```

**Success Criteria**:

- Can execute any shell command and connect via stdio
- Proper stdio communication with JSON-RPC protocol
- Connection pooling for multiple tool calls
- Resource cleanup when connections close

### Phase 3: HTTP Transport

**Duration**: 1-2 weeks

**Deliverables**:

- HTTP-based MCP client transport using MCP Kotlin SDK
- HTTP support in the `Mcp server` command for bidirectional HTTP MCP servers
- Authentication and header management
- Connection management and retries

#### HTTP Transport Architecture

The MCP Kotlin SDK provides HTTP transport support via `StreamableHttpClientTransport`.

**Implementation**:

```kotlin
class HttpMcpTransport(
    private val baseUrl: String,
    private val headers: Map<String, String> = emptyMap(),
    private val authToken: String? = null
) : McpClientTransport {

    private var client: Client? = null

    suspend fun connect(): Boolean {
        client = Client(clientInfo = Implementation("specscript-client", "1.0.0"))

        val httpClient = HttpClient(CIO) {
            if (authToken != null) {
                install(Auth) {
                    bearer {
                        loadTokens { BearerTokens(authToken, "") }
                    }
                }
            }
        }

        val transport = StreamableHttpClientTransport(
            httpClient = httpClient,
            baseUrl = baseUrl,
            headers = headers
        )

        client?.connect(transport)
        return true
    }

    override suspend fun callTool(request: CallToolRequest): CallToolResult {
        return client?.callTool(request)
            ?: throw IllegalStateException("Client not connected")
    }
}
```

**Usage Examples**:

```yaml specscript
# HTTP transport
Call Mcp tool:
  transport:
    type: http
    url: "https://api.example.com/mcp"
    headers:
      Authorization: "Bearer ${API_TOKEN}"
      Content-Type: "application/json"
  tool: generate_code
  arguments:
    prompt: "Write a hello world function"

# HTTP with custom headers
Call Mcp tool:
  transport:
    type: http
    url: "http://localhost:8080/mcp"
    headers:
      X-Custom-Header: "value"
  tool: local_tool
  arguments:
    data: "test data"
```

**Success Criteria**:

- HTTP transport works with real MCP servers
- Proper authentication and header handling
- Streaming responses handled correctly
- Connection retry and error recovery

### Phase 4: Cleanup and Polish

**Duration**: 1 week

**Deliverables**:

- Resource cleanup and leak prevention for all transport types
- Error handling refinement and comprehensive error messages
- Performance optimization for transport layer
- Documentation updates and example refinement
- Integration testing across all transport types

**Resource Management**:

```kotlin
interface McpClientTransport {
    suspend fun connect(): Boolean
    suspend fun callTool(request: CallToolRequest): CallToolResult
    suspend fun listTools(): ListToolsResult
    suspend fun close() // Critical for resource cleanup
}

// Proper resource cleanup in all implementations
class StdioTransport : McpClientTransport {
    override suspend fun close() {
        client?.close()
        process?.destroy()
        process?.waitFor(5, TimeUnit.SECONDS) ?: process?.destroyForcibly()
    }
}
```

**Error Handling Improvements**:

- Standardized error messages across all transport types
- Proper exception chaining with original cause preservation
- Timeout handling with graceful degradation
- Resource cleanup in error conditions

## Integration with Existing Commands

### Command Enhancements

**Enhanced `Call Mcp tool` Command**:

```yaml specscript
Call Mcp tool:
  # Connection specification
  server: my-server              # For in-process or registered servers

  # Transport options
  transport:
    type: [ internal|stdio|http ]

    # Stdio-specific
    command: python server.py --verbose

    # HTTP-specific
    url: "https://api.example.com/mcp"
    headers: { Authorization: "Bearer token" }

    # Connection management
    timeout: 30000              # Connection timeout in ms

  # Tool execution
  tool: my-tool
  arguments: { key: value }

  # Output options
  format: [ json|yaml|text ]      # Response format
```

### Error Handling Strategy

**Transport-Specific Error Handling**:

- **Stdio Transport**: Handle process startup failures, stdio communication errors
- **HTTP Transport**: Handle network timeouts, authentication failures, HTTP status codes
- **Resource Management**: Handle connection drops, cleanup on failures

**Error Categories**:

1. **Transport Errors**: Connection failures, protocol errors
2. **Server Errors**: MCP server errors, tool not found, invalid arguments
3. **Tool Execution Errors**: SpecScript errors within tool execution
4. **Resource Errors**: Out of memory, file system errors, process limits

## Testing Strategy

### Unit Testing

- Transport abstraction interfaces
- Error handling scenarios
- Resource cleanup verification

### Integration Testing

- Real MCP server communication across all transport types
- Server export and process spawning
- Long-running connection scenarios
- Performance under load

### Specification Testing

- All transport types with working examples
- Error scenarios with proper error propagation
- Connection lifecycle management
- Performance benchmarks

### Test Infrastructure Setup

```yaml specscript
# Test servers for different transports
Test case: Process transport with SpecScript server
  setup:
    Register mcp server:
      name: test-process-server
      tools: { echo: { script: "Output: ${input.message}" } }

  test:
    Call Mcp tool:
      server: test-process-server
      transport: { type: process }
      tool: echo
      arguments: { message: "Hello Process" }

    Expected output: Hello Process

  cleanup:
    Close mcp connection:
      server: test-process-server

Test case: HTTP transport with mock server
  setup:
    Start mock http mcp server:
      port: 18080

  test:
    Call Mcp tool:
      transport:
        type: http
        url: "http://localhost:18080/mcp"
      tool: mock_tool

  cleanup:
    Stop mock http mcp server:
      port: 18080
```

## Risk Assessment & Mitigation

### Technical Risks

**Risk**: Process spawning complexity and resource management

- **Likelihood**: Medium
- **Impact**: High
- **Mitigation**: Comprehensive process lifecycle management, resource cleanup, timeouts

**Risk**: HTTP transport compatibility with various MCP server implementations

- **Likelihood**: Medium
- **Impact**: Medium
- **Mitigation**: Use official MCP Kotlin SDK transports, extensive testing with real servers

**Risk**: Connection pooling and session management complexity

- **Likelihood**: High
- **Impact**: Medium
- **Mitigation**: Simple connection registry initially, iterate based on usage patterns

### Integration Risks

**Risk**: Breaking existing in-process functionality during refactoring

- **Likelihood**: Low
- **Impact**: High
- **Mitigation**: Transport abstraction preserves existing behavior, comprehensive regression testing

**Risk**: Performance degradation with multiple transport types

- **Likelihood**: Medium
- **Impact**: Low
- **Mitigation**: Performance benchmarking, connection reuse, lazy initialization

## Success Criteria

### Functional Success

- [ ] All four transport types (internal, process, http, sse) work with real MCP servers
- [ ] Can spawn SpecScript servers as separate processes with stdio communication
- [ ] Can connect to external MCP servers (Python, Node.js, etc.)
- [ ] Connection pooling and reuse works correctly
- [ ] Proper resource cleanup prevents memory/process leaks

### Quality Success

- [ ] Comprehensive specification documentation with working examples
- [ ] Unit and integration test coverage > 90%
- [ ] Performance meets or exceeds HTTP client patterns
- [ ] Error handling provides clear, actionable feedback
- [ ] No resource leaks under normal and error conditions

### Adoption Success

- [ ] MCP server testing workflows utilize process-based stdio transport
- [ ] External MCP server integration examples work out of the box
- [ ] HTTP-based MCP services can be consumed declaratively
- [ ] Community feedback validates utility for MCP development

## Implementation Timeline

**Total Duration**: 4-5 weeks

- **Phase 1** (Weeks 1-2): Transport abstraction layer
- **Phase 2** (Weeks 2-3): Stdio transport (via shell commands)
- **Phase 3** (Weeks 3-4): HTTP transport (client and server)
- **Phase 4** (Week 5): Cleanup and polish

**Milestone Checkpoints**:

- Week 2: Transport abstraction complete, internal transport refactored
- Week 3: Shell command execution with stdio MCP protocol communication
- Week 4: HTTP transport working with real MCP servers, HTTP MCP server support
- Week 5: Resource cleanup, error handling polish, and production readiness

## Future Considerations

### WebSocket Transport (Future)

- Add WebSocket transport for bidirectional communication if needed
- Support for server-initiated notifications and updates

### Authentication & Security

- OAuth2 flow support for HTTP transports
- Certificate-based authentication for secure environments
- API key management and rotation

### Performance Optimizations (Future)

- Connection pooling across multiple SpecScript executions
- Persistent connection daemon for development workflows
- Batch request support for multiple tool calls

### Monitoring & Observability (Future)

- Connection metrics and health checks
- Request/response logging and tracing
- Performance profiling and bottleneck identification

## Conclusion

This plan provides a comprehensive approach to implementing proper MCP client transport support while addressing the
unique challenges of SpecScript's dynamic server definitions and same-process execution model. The phased approach
minimizes risk while delivering incremental value, and the transport abstraction ensures extensibility for future
transport types.

The implementation will transform SpecScript from having basic MCP server testing capabilities to being a full-featured
MCP client that can integrate with the entire MCP ecosystem, including external servers and HTTP-based services.