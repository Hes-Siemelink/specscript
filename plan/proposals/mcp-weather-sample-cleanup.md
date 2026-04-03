# MCP Weather Sample Cleanup

## Problem

The `samples/mcp/weather/` directory has a single `weather.spec.yaml` that defines an MCP server with tools but never
demonstrates calling those tools from a client. The file name is generic and doesn't hint at what it does. There's also
a `summarize.spec.yaml` that reads a local YAML file but isn't connected to MCP at all. The sample doesn't tell a
clear story of "start a server, then call it."

## Proposed Changes

### 1. Rename `weather.spec.yaml` to `start-server.spec.yaml`

The main script starts an MCP server — its name should say so. This also makes it work nicely with `spec -i .` 
interactive mode, where the user picks from file names.

### 2. Simplify the server to two tools

Remove `get_alerts2` (duplicate of `get_alerts` via file reference) and `get_forecast` (stub that returns a placeholder
string). Keep `hello` and `get_alerts` — one trivial, one real. This makes the sample focused and easy to follow.

### 3. Add `call-hello.spec.yaml` — client script

A simple client script that connects to the running server and calls the `hello` tool:

```yaml
Mcp tool call:
  tool: hello
  server:
    url: http://localhost:8080/mcp
  input:
    name: World
```

This demonstrates the client side of MCP and gives users a copy-paste starting point.

### 4. Add `call-alerts.spec.yaml` — client script for the real tool

A client script that calls `get_alerts` with a state code:

```yaml
Input parameters:
  state: NY

Mcp tool call:
  tool: get_alerts
  server:
    url: http://localhost:8080/mcp
  input:
    state: ${state}
```

This shows parameterized tool calls and end-to-end MCP usage with a real API.

### 5. Remove `summarize.spec.yaml`

This script reads a local YAML file and formats it — it's not MCP-related. It was likely a development aid for 
testing the alert output format. Remove it.

### 6. Keep `sample.yaml` and `get-alert.spec.yaml`

`sample.yaml` is useful recorded data for offline testing/development of the alert tool. `get-alert.spec.yaml` is
the standalone script referenced by the server's tool registration — it stays.

### Result

```
samples/mcp/weather/
  start-server.spec.yaml    # Start MCP server with hello + get_alerts tools
  call-hello.spec.yaml      # Client: call the hello tool  
  call-alerts.spec.yaml     # Client: call the get_alerts tool with a state code
  get-alert.spec.yaml       # Standalone alert script (also used by server tool)
  sample.yaml               # Recorded weather API response data
```

The user workflow becomes: run `start-server`, then in another terminal run `call-hello` or `call-alerts`.
