---
# specscript-km2e
title: 'TypeScript: Directory --help doesn''t print description from README.md'
status: completed
type: bug
priority: normal
created_at: 2026-03-31T05:43:57Z
updated_at: 2026-03-31T20:03:18Z
---

The TypeScript implementation doesn't output the directory description from README.md or specscript-config.yaml when listing directory contents. The Kotlin implementation does. This causes the Cli.spec.md and Organizing spec tests to fail in TypeScript.

## Root Cause\n\nextractDescriptionFromMarkdown() in typescript/src/cli.ts requires a # heading before it collects description text. README files without headings (like my-scripts/README.md which is just 'A collection of simple scripts') return undefined.\n\nKotlin's SpecScriptMarkdown.description grabs the first non-blank, non-header line from the first text block — no heading required.\n\n## Fix\n\nUpdate extractDescriptionFromMarkdown to collect the first paragraph of text, skipping headings/comments/blank lines, without requiring a heading first.

## Summary of Changes

Fixed extractDescriptionFromMarkdown() in typescript/src/cli.ts to match Kotlin behavior: skip blank lines, headings, comments, and code fences, then return the first real text line. No longer requires a # heading before collecting description text. Both previously failing spec tests now pass.
