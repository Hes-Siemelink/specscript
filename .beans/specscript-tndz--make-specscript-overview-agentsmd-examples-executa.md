---
# specscript-tndz
title: Make specscript-overview-agents.md examples executable
status: completed
type: task
priority: normal
created_at: 2026-03-21T07:34:37Z
updated_at: 2026-03-21T07:36:36Z
---

Convert illustrative yaml blocks to yaml specscript with Code example headers. Replace command reference table with link to commands/README.md. Use localhost:2525 sample server for HTTP examples.

\n## Summary of Changes\n\nExpanded HTTP section from 2 to 8 executable examples using sample server endpoints: GET with defaults, POST with body, custom headers via echo/header, query parameters via echo/query, PUT, DELETE, POST echo/body, and chaining requests with As using /greetings. Replaced command reference table with link to commands/README.md. All specificationTest tests pass.
