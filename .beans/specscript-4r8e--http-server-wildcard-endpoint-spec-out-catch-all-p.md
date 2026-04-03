---
# specscript-4r8e
title: 'Http server wildcard endpoint: spec out catch-all path using OpenAPI convention'
status: todo
type: feature
created_at: 2026-04-03T05:45:35Z
updated_at: 2026-04-03T05:45:35Z
---

The current catch-all endpoint uses "{...}" which is Ktor implementation leaking into the spec. This should be properly specified using OpenAPI conventions (e.g. /** or a dedicated wildcard syntax). Needs: spec update in Http server.spec.md, schema update, implementation change in HttpServer.kt normalizePath, and update all samples using catch-all endpoints.
