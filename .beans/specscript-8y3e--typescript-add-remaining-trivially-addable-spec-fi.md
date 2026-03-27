---
# specscript-8y3e
title: 'TypeScript: Add remaining trivially-addable spec files'
status: completed
type: task
priority: low
created_at: 2026-03-27T10:04:09Z
updated_at: 2026-03-27T13:07:46Z
---

Add 2 spec files that need no new commands: (1) language/Testing.spec.md — skip the first section (spec --test . hangs), the Tests with setup/teardown section uses only implemented commands. (2) cli/Configuration files.spec.md — no executable code blocks, pure prose. XS effort, adds marginal coverage completeness.

## Summary of Changes\n\nAdded language/Testing.spec.md to TypeScript spec runner (Level 2). The first section (spec --test .) is skipped to avoid hanging. The Setup and teardown section with Before all tests/Tests/After all tests runs successfully. Configuration files.spec.md was not added as it has no executable blocks. Test count: 397 passed, 18 skipped.
