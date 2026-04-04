# Command: Http endpoint

`Http endpoint` adds endpoints to an existing HTTP server. Use it to define endpoints separately from the server
definition, analogous to how `Mcp tool` adds tools to an MCP server.

| Input     | Supported    |
|-----------|--------------|
| Value     | no           |
| List      | auto-iterate |
| Object    | yes          |
| Raw input | yes          |

[Http endpoint.schema.yaml](schema/Http%20endpoint.schema.yaml)

## Basic usage

First start a server, then add endpoints with `Http endpoint`:

```yaml specscript
Code example: Add endpoint to running server

Http server:
  name: endpoint-demo
  port: 25010

Http endpoint:
  /hello:
    get:
      output: Hello from endpoint!

GET: http://localhost:25010/hello

Expected output: Hello from endpoint!

Stop http server: endpoint-demo
```

The endpoint format is identical to the `endpoints` property in `Http server` — paths with method handlers.

## Adding multiple endpoints

You can call `Http endpoint` multiple times to build up the server incrementally:

```yaml specscript
Code example: Multiple endpoint calls

Http server:
  name: multi-server
  port: 25011

Do:
  - Http endpoint:
      /users:
        get:
          output:
            - Alice
            - Bob

  - Http endpoint:
      /status:
        get:
          output: ok

GET: http://localhost:25011/users

Expected output:
  - Alice
  - Bob
---
GET: http://localhost:25011/status

Expected output: ok

Stop http server: multi-server
```

## Script handlers

Endpoints support the same handler types as `Http server` — `output`, inline `script`, and script file references:

```yaml specscript
Code example: Endpoint with inline script

Http server:
  name: script-endpoint-server
  port: 25012

Http endpoint:
  /greet:
    get:
      script:
        Output: Hello ${input.name}!

GET: http://localhost:25012/greet?name=Alice

Expected output: Hello Alice!

Stop http server: script-endpoint-server
```

## Which server receives the endpoints

`Http endpoint` adds to the most recently started or referenced `Http server` in the current script. If you need to
target a specific server, call `Http server` with that name before defining the endpoints.
