---
# specscript-6pnu
title: 'TypeScript: Implement Validate schema command'
status: todo
type: feature
priority: normal
created_at: 2026-03-27T10:03:45Z
updated_at: 2026-03-27T10:03:45Z
---

Implement Validate schema command in TypeScript. Unlocks 3 spec files: Validate schema.spec.md, Validate tests.spec.yaml, Schema tests.spec.yaml (currently in SKIP_FILES). Requires JSON Schema validation — user previously chose to skip because control-flow Schema tests need full Draft 2020-12 (refs, oneOf, not, unevaluatedProperties). The simpler Validate schema.spec.md and Validate tests.spec.yaml only use basic schemas. Consider implementing basic-only validation or using a library.
