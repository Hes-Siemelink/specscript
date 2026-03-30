---
# specscript-oew3
title: Assess CLI specification files for inconsistencies and completeness
status: completed
type: task
priority: normal
created_at: 2026-03-30T17:32:27Z
updated_at: 2026-03-30T17:43:37Z
---

Review all files in specification/cli/ for inconsistencies, gaps, and completeness

## Assessment

### Files Reviewed

| File | Lines | Purpose |
|------|-------|---------|
| README.md | 8 | Index page for CLI spec section |
| Running SpecScript files.spec.md | 125 | How to run files and directories |
| Command line options.spec.md | 156 | Behavior of each global flag |
| Configuration files.spec.md | 10 | ~/.specscript folder and credentials |
| specscript-command-line-options.yaml | 35 | Canonical option definitions (SSOT) |

### Findings

- [x] F6: Remove Configuration files.spec.md stub
- [x] F1: Remove broken links from README.md
- [x] F2: Fix stale command name in Running SpecScript files
- [x] F3: Fix inconsistent directory listing format
- [x] F7: Fix grammar issue in Running SpecScript files
- [x] F4: Leave YAML schema as-is
- [x] F5: Leave test coverage as-is
- [x] F8: Keep --output behavior as-is

## Summary of Changes

- Removed stub Configuration files.spec.md
- Removed broken links (Library usage, Error handling) from README.md
- Fixed stale command name simple-question → prompt in Running SpecScript files.spec.md
- Made directory listing block executable (shell cli + output) matching the format in Command line options.spec.md
- Fixed grammar: an SpecScript → a SpecScript
- All specification tests pass
