---
# specscript-djio
title: 'MCP server: tools as list of scripts'
status: completed
type: feature
priority: normal
created_at: 2026-04-03T20:56:57Z
updated_at: 2026-04-03T21:01:34Z
---

When tools is a YAML list of filenames, each file becomes a tool. Tool name is filename without extensions. Metadata derived from Script info and Input schema.

## Summary of Changes\n\nImplemented tools-as-list-of-scripts for Mcp server. When tools is a YAML list of filenames, each file becomes a tool with its name derived from the filename (sans extensions). Normalization happens before deserialization in both Kotlin and TypeScript. Schema updated with oneOf to accept both object and array forms. Port numbers in Mcp server.spec.md renumbered to 8080-8084 (sequential, unique).
