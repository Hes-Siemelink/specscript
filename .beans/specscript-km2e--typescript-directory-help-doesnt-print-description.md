---
# specscript-km2e
title: 'TypeScript: Directory --help doesn''t print description from README.md'
status: todo
type: bug
created_at: 2026-03-31T05:43:57Z
updated_at: 2026-03-31T05:43:57Z
---

The TypeScript implementation doesn't output the directory description from README.md or specscript-config.yaml when listing directory contents. The Kotlin implementation does. This causes the Cli.spec.md and Organizing spec tests to fail in TypeScript.
