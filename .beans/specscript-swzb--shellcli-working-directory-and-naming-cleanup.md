---
# specscript-swzb
title: Shell/CLI working directory and naming cleanup
status: completed
type: feature
priority: normal
created_at: 2026-04-01T19:03:37Z
updated_at: 2026-04-01T20:19:02Z
---

Proposal for cleaning up shell cli defaults: working directory, naming, and PWD variable

## Proposal\n\nProposal written to plan/proposals/shell-cli-cleanup.md\n\n- [x] Write proposal (plan/proposals/shell-cli-cleanup.md)\n- [x] Review proposal with user — decisions captured\n- [x] Write/update specs for PWD variable (item 2)\n- [x] Write/update specs for SCRIPT_HOME env var fix\n- [x] Write/update specs for cli directive rename\n- [x] Write/update specs for temp-file= directive rename\n- [x] Write/update specs for Shell/Cli default working dir change\n- [x] Implement: PWD variable (Kotlin + TypeScript)\n- [x] Implement: SCRIPT_HOME env var fix (Kotlin + TypeScript)\n- [x] Implement: cli directive (Kotlin + TypeScript)\n- [x] Implement: temp-file= directive (Kotlin + TypeScript)\n- [x] Implement: Shell/Cli default working dir (Kotlin + TypeScript)\n- [x] Migrate all spec files (shell cli → cli, file= → temp-file=)

## Summary of Changes

All 5 changes from the proposal are implemented in both Kotlin and TypeScript:

1. Default working directory for markdown Shell and Cli directives changed to SCRIPT_HOME
2. PWD variable added (resolves to process current working directory)
3. shell cli directive renamed to cli (old form removed)
4. yaml file= directive renamed to yaml temp-file= (old form removed)
5. SCRIPT_HOME env var bug fixed in Shell subprocess

All spec files migrated. ~41 cli blocks got cd=${SCRIPT_TEMP_DIR} added, ~7 blocks had redundant cd=${SCRIPT_HOME} removed. Documentation updated in SpecScript Markdown Documents.spec.md, Cli.spec.md, CLAUDE.md, and SKILL.md. All Kotlin (504) and TypeScript (423) tests pass.
