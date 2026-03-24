---
# specscript-qx0g
title: Add name to Http server as required identifier, port optional
status: completed
type: feature
priority: normal
created_at: 2026-03-24T05:42:58Z
updated_at: 2026-03-24T05:53:31Z
---

Make Http server use name as the required identifier (matching Mcp server pattern). Port becomes optional with a default. Registry re-keyed by name. Stop by name instead of port.

- [x] Write spec changes (Http server.spec.md)
- [x] Update schema (Http server.schema.yaml)
- [x] Update implementation (HttpServer.kt)
- [x] Update test file (Http server tests.spec.yaml)
- [x] Build and run all tests

## Summary of Changes

Http server now uses name as the required identifier (matching Mcp server). Port is optional, defaulting to 3000. Registry re-keyed by name. Stop by name. All 474+ specification tests pass.

Files changed:
- specification/commands/core/http/Http server.spec.md
- specification/commands/core/http/schema/Http server.schema.yaml
- specification/commands/core/http/tests/Http server tests.spec.yaml
- src/main/kotlin/specscript/commands/http/HttpServer.kt
- src/tests/specification/specscript/spec/SpecScriptTestSuite.kt
- README.md
- samples/http-server/sample-server/sample-server.spec.yaml
- samples/test/setup-teardown.spec.yaml
- samples/http-server/simple-proxy/recording-proxy.spec.yaml
- samples/http-server/simple-proxy/replaying-proxy.spec.yaml
- samples/digitalai/platform/cloud-connector/tests/cloud-connector-tests.spec.yaml
