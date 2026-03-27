---
# specscript-70ce
title: 'Bug: spec-ts -i . does not show subdirectories'
status: completed
type: bug
priority: normal
created_at: 2026-03-27T16:57:36Z
updated_at: 2026-03-27T17:04:13Z
---

When running spec-ts -i . in the samples directory, only file-based commands (hello) are shown. Kotlin spec -i . shows all 12+ subdirectories (basic, digitalai, goals-app, etc). Root cause: getDirectoryCommands() in cli.ts only scans for .spec.yaml/.spec.md files, never checks for subdirectories.

## Summary of Changes\n\nFixed getDirectoryCommands() in typescript/src/cli.ts to scan for subdirectories in addition to spec files. Added hasCliCommands() helper that recursively checks whether a directory contains any .spec.yaml or .spec.md files. Subdirectories named 'tests' are excluded, matching Kotlin behavior. Output now matches Kotlin spec exactly.
