---
# specscript-ir46
title: Rename 'Before tests' to 'Before all tests' and 'After tests' to 'After all tests'
status: completed
type: task
priority: normal
created_at: 2026-03-24T05:34:35Z
updated_at: 2026-03-24T05:39:11Z
---

Rename testing commands across specification, implementation, samples, schemas, and skills.

## Summary of Changes

Renamed 'Before tests' to 'Before all tests' and 'After tests' to 'After all tests' across:
- Kotlin source (BeforeTests.kt, AfterTests.kt, TestUtil.kt comment)
- Spec files renamed and content updated (Before all tests.spec.md, After all tests.spec.md)
- Schema files renamed (Before all tests.schema.yaml, After all tests.schema.yaml)
- Tests.spec.md, Test case.spec.md, Testing.spec.md updated
- Overview agents doc updated (links + content)
- All sample YAML files updated (9 files)
- SKILL.md for specscript-tests updated
- All tests pass.
