---
# specscript-qen4
title: Improve levels.yaml based on alignment analysis
status: completed
type: task
priority: normal
created_at: 2026-03-28T06:51:31Z
updated_at: 2026-03-28T06:57:19Z
---

Implement recommendations from level-alignment.md: add skip-sections, fix requires/dependency graph, add kernel concept, fix Assignment mapping, track ParameterData.schema.yaml, move On error/On error type to L0, move contaminated test cases.

## Plan

- [x] Move On error, On error type from L1 to L0 (rec C)
- [x] Move contaminated test cases to correct levels (rec A)
- [x] Add skip-sections for mixed-level spec.md files (rec B)
- [x] Add kernel concept and dependency graph to header (rec F, I)
- [x] Fix Assignment mapping comment (rec L)
- [x] Track ParameterData.schema.yaml (rec K)
- [x] Rewrite Before all tests example to use L0 commands (rec D)
- [x] Add L0 example to Answers.spec.md (rec E)
- [x] Run specificationTest to verify nothing breaks

## Summary of Changes

Implemented recommendations A–F, H–L from the level-alignment analysis:

- Moved On error and On error type from Level 1 to Level 0 (they are inseparable from Error)
- Moved 4 contaminated test cases to correct levels (3 new split files)
- Added skip-sections annotations for 11 spec.md files with mixed-level sections
- Added kernel concept and dependency graph to the header comment
- Fixed dependency graph: MCP requires [1] not [4], HTTP has no requires (independent of L3)
- Added Assignment command registry mapping comment
- Added ParameterData.schema.yaml to tracked shared schemas
- Rewrote Before all tests example to use only L0 commands (HTTP example kept as secondary)
- Added L0-compatible Answers example using Input parameters (Prompt example kept as secondary)
- All 486+ specification tests pass
