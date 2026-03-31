---
# specscript-0o24
title: Inline sample references in 3 spec files
status: completed
type: task
priority: normal
created_at: 2026-03-31T04:53:07Z
updated_at: 2026-03-31T04:56:11Z
---

Phase 1 of move-reference-samples: rewrite 3 spec files to use file= blocks instead of cd=samples

## Todo

- [x] Organizing SpecScript files in directories.spec.md — rewrite lines 1-60 with file= blocks, fix stale listing
- [x] Run script.spec.md — change to use resource: subdir/ instead of file: samples/basic/...
- [x] Cli.spec.md — create local dir with file= blocks, remove cd: samples
- [x] Run specificationTest to verify — all 506 tests pass, full build passes
