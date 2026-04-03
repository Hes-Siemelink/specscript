---
# specscript-9n4p
title: Clean up MCP weather sample
status: completed
type: task
priority: normal
created_at: 2026-04-03T05:48:48Z
updated_at: 2026-04-03T05:56:58Z
---

Rename weather.spec.yaml to start-server.spec.yaml, simplify to 2 tools, add client scripts call-hello and call-alerts, remove summarize.spec.yaml. See plan/proposals/mcp-weather-sample-cleanup.md

## Summary of Changes\n\nRenamed weather.spec.yaml to start-server.spec.yaml. Simplified to 2 tools (hello, get_alerts). Added call-hello.spec.yaml and call-alerts.spec.yaml client scripts. Removed summarize.spec.yaml and duplicate/stub tools.
