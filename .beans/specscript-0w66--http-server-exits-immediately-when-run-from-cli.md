---
# specscript-0w66
title: Http server exits immediately when run from CLI
status: completed
type: bug
priority: normal
created_at: 2026-03-31T20:08:43Z
updated_at: 2026-03-31T20:09:42Z
---

Running spec sample-server causes the JVM to exit immediately because the Http server starts with wait=false and Netty's threads are daemon threads. The MCP server solved this with a non-daemon keep-alive thread. The Http server needs the same treatment.

## Summary of Changes

Added a non-daemon keep-alive thread per Http server instance, matching the pattern already used by McpServer. The thread blocks on Thread.join() and is interrupted when stop() is called. This keeps the JVM alive while the server is running.
