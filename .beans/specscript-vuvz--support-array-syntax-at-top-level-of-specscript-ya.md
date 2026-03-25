---
# specscript-vuvz
title: Support array syntax at top level of SpecScript YAML files
status: completed
type: feature
priority: normal
created_at: 2026-03-25T19:18:35Z
updated_at: 2026-03-25T19:26:35Z
---

Allow top-level arrays in .spec.yaml files so users can write: - Print: Hello\n- Print: world\ninstead of requiring --- separators or Do command wrapper.

## Todo\n\n- [x] Update spec document with array syntax section\n- [x] Implement toCommandList array handling in Script.kt\n- [x] Build and run tests

## Summary of Changes\n\nAdded top-level array syntax support to SpecScript YAML files.\n\n- Script.kt: toCommandList now handles ArrayNode by recursively processing each element\n- Spec updated with array syntax example in the 'Multiple commands' section\n- All 483 specification tests pass
