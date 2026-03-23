---
# specscript-27ji
title: Align platform/connect scripts with release/credentials
status: completed
type: task
priority: normal
created_at: 2026-03-23T20:53:21Z
updated_at: 2026-03-23T20:55:03Z
---

Rename files and align structural patterns in samples/digitalai/platform/connect to match release/credentials conventions.

- [ ] Rename create-new-account.spec.yaml to create.spec.yaml
- [ ] Rename delete-account.spec.yaml to delete.spec.yaml
- [ ] Rename select-account.spec.yaml to select-default.spec.yaml
- [ ] Align login.spec.yaml (Create reference)
- [ ] Align delete.spec.yaml (Output vs Print)
- [ ] Align select-default.spec.yaml (text, Output)
- [ ] Align create.spec.yaml (description text)
- [ ] Fix get-token.spec.yaml (remove debug Print, fix spec.yaml variable typo)
- [ ] Update specscript-config.yaml description
- [ ] Check for internal references to old filenames
