---
# specscript-m825
title: 'Dot-in-property-names: bracket notation (Option B only)'
status: completed
type: feature
priority: high
created_at: 2026-03-21T08:21:38Z
updated_at: 2026-03-21T08:22:53Z
---

Implement bracket notation for accessing properties with dots in names. Alternative to backslash escaping, on separate branch for comparison.

## Summary of Changes\n\nImplemented bracket notation ["key"] for accessing properties with dots (or spaces) in names.\n\n- Variables.kt: segment-based parsePath() handles ["key"] quoted brackets vs [N] numeric indexes\n- VariablesTest.kt: 4 bracket notation test cases\n- Variables.spec.md: concise spec example\n- tests/Variables tests.spec.yaml: 4 edge case tests (nested, multiple dots, spaces, mixed with array index)\n\nAll 462 tests pass. splitIntoVariableAndPath unchanged — existing regex already splits at first [.
