---
# specscript-p9gf
title: Analyze spec-level alignment
status: completed
type: task
priority: normal
created_at: 2026-03-28T06:25:14Z
updated_at: 2026-03-28T06:31:40Z
---

Analyze how specification documents align with the levels system. Both directions: specs may need restructuring, level boundaries may need adjusting. Use go-implementer-guide, implementation reports, and the specs themselves.

## Summary of Changes\n\nWrote plan/proposals/level-alignment.md. Analyzed all 142 spec files against their assigned levels, found 29 cross-level contaminations across 18 files. Categorized into 4 types: testing scaffolding (benign), CLI showcase sections (main vector), inherent dependencies (unavoidable), and test file contamination (fixable). Produced 7 recommendations: move error handling to L0, move 5 test cases to correct levels, add skip-sections to levels.yaml, rewrite Before all tests and Answers examples, formalize the kernel concept, accept L2 Markdown contamination.
