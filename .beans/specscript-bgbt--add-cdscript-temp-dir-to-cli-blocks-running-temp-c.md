---
# specscript-bgbt
title: Add cd=${SCRIPT_TEMP_DIR} to cli blocks running temp-created files
status: completed
type: task
priority: normal
created_at: 2026-04-01T20:09:09Z
updated_at: 2026-04-01T20:11:29Z
---

After changing default working directory to SCRIPT_HOME, cli blocks that run temp-created files need explicit cd=${SCRIPT_TEMP_DIR}. Update all affected spec files.

## Tasks

- [x] SpecScript Yaml Scripts.spec.md (lines 18, 83, 161, 187, 200, 225)
- [x] Directory tests.spec.md (lines 17, 55)
- [x] Testing.spec.md (skipped - no temp files, spec --test . works from SCRIPT_HOME) (line 5)
- [x] Packages.spec.md (lines 35, 74, 133, 170, 199, 234, 259, 284, 312, 398, 464, 479)
- [x] Organizing SpecScript files in directories.spec.md (lines 143, 217)
- [x] Script info.spec.md (line 71)
- [x] Input parameters.spec.md (line 66)
- [x] Input schema.spec.md (line 64)
- [x] Connect to.spec.md (line 60)
- [x] Command line options.spec.md (lines 10, 110, 127)
- [x] README.md (lines 22, 65, 77, 100, 232, 249, 297, 314)
- [x] README-old.md (lines 42, 54, 71, 165, 215)

## Summary of Changes

Added cd=${SCRIPT_TEMP_DIR} to all bare cli blocks that run temp-created files across 11 spec files. Total of 41 cli blocks updated. Skipped Testing.spec.md (no temp files; spec --test . works from SCRIPT_HOME). Left bare cli blocks that run spec with no args or --help-only without file references.
