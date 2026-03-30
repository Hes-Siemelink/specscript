---
# specscript-u6wo
title: Replace --output with --no-output
status: completed
type: feature
priority: normal
created_at: 2026-03-30T18:47:16Z
updated_at: 2026-03-30T18:49:59Z
---

Remove the no-op --output/-o flag and add --no-output/-n to suppress end-of-script result printing

## Plan

- [x] Update spec: specscript-command-line-options.yaml
- [x] Update spec: Command line options.spec.md
- [x] Update spec: Running SpecScript files.spec.md
- [x] Update spec: SpecScript Yaml Scripts.spec.md (remove --output from test)
- [x] Update spec: Cli.spec.md (banner output)
- [x] Update spec: SpecScript Markdown Documents.spec.md (banner output, 2 places)
- [x] Implement: Kotlin CliCommandLineOptions.kt
- [x] Implement: Kotlin SpecScriptCli.kt (no changes needed)
- [x] Implement: TypeScript cli.ts
- [x] Run specificationTest and fix any failures

## Summary of Changes

Removed the no-op --output/-o flag and replaced it with --no-output/-n which suppresses end-of-script result printing. Updated the YAML option definitions, both Kotlin and TypeScript implementations, and all 7 spec files containing the banner output. Also updated the agents overview doc.
