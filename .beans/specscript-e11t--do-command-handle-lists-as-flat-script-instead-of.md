---
# specscript-e11t
title: 'Do command: handle lists as flat script instead of auto-mapping'
status: completed
type: feature
priority: normal
created_at: 2026-03-25T21:04:27Z
updated_at: 2026-03-25T21:41:58Z
---

Change Do to handle arrays natively (AnyHandler) instead of relying on auto-list mapping. A Do with a list should run sequentially and return the final output, not collect outputs into an array.

## Todo

- [ ] Update Do.spec.md: change content type table, rewrite 'Capture the output' section
- [ ] Update Do tests.spec.yaml: fix expectations
- [ ] Update Kotlin Do.kt: change to AnyHandler
- [ ] Run specificationTest to verify
- [ ] Commit spec + Kotlin changes (no TypeScript)

## Summary of Changes\n\nDo command changed from ObjectHandler to AnyHandler, handling lists as flat scripts. Committed on main as 62488d6.
