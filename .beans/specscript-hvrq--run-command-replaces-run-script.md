---
# specscript-hvrq
title: Run command (replaces Run script)
status: in-progress
type: feature
priority: normal
created_at: 2026-04-04T07:24:45Z
updated_at: 2026-04-04T07:24:53Z
---

Rename Run script to Run and extend with cd, inline script, and file properties. Unifies script execution patterns. See plan/proposals/run-in-command.md

## Plan

- [x] Write proposal (plan/proposals/run-in-command.md)
- [x] Write draft specs (plan/draft-specs/)
- [ ] User review of draft specs
- [ ] Write Run.schema.yaml in specification/
- [ ] Implement Run command (rename + extend RunScript.kt)
- [ ] Implement command delegation for inline script form
- [ ] Update existing Run script.spec.md or replace with Run.spec.md
- [ ] Add Running SpecScript from SpecScript.spec.md to specification/language/
- [ ] Add Run script alias in CommandLibrary
- [ ] Add resource as deprecated alias for script
- [ ] Run tests
- [ ] Update bean, report, commit

## Draft specs

- plan/draft-specs/Run.spec.md — command reference
- plan/draft-specs/schema/Run.schema.yaml — JSON schema
- plan/draft-specs/Running SpecScript from SpecScript.spec.md — isolation ladder (language-level)
