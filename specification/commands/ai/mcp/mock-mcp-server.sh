#!/bin/bash

# Mock MCP server that handles initialization and tool calls
# Returns proper responses for MCP protocol initialization

first_request=true

while IFS= read -r line; do
    if [ "$first_request" = true ]; then
        # First request should be initialize - return initialize response
        echo '{"jsonrpc": "2.0", "id": 1, "result": {"protocolVersion": "2024-11-05", "capabilities": {"tools": {}}, "serverInfo": {"name": "mock-server", "version": "1.0.0"}}}'
        first_request=false
    else
        # Subsequent requests - return tool call response
        echo '{"jsonrpc": "2.0", "id": 2, "result": {"content": [{"type": "text", "text": "Mock server response"}]}}'
        break
    fi
done