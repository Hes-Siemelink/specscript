---
# specscript-5how
title: Unify Http server handler model with MCP server
status: completed
type: feature
priority: normal
created_at: 2026-03-24T05:58:30Z
updated_at: 2026-03-24T06:01:32Z
---

Remove the file property from Http server handlers. Make script work like MCP: string value = file reference, object value = inline script. Keep output as-is.

- [x] Update spec (Http server.spec.md)
- [x] Update schema (Http server.schema.yaml)
- [x] Update implementation (HttpServer.kt)
- [x] Update tests (Http server tests.spec.yaml)
- [x] Update samples that use file handlers (none needed - string shorthand unchanged at YAML level)
- [x] Build and run all tests

## Summary of Changes

Removed file property from Http server handlers. The script property now works like MCP server: string value = file reference, object value = inline script. The string shorthand (e.g. post: handler.spec.yaml) still works, now internally mapping to script instead of file.
