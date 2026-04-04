---
# specscript-8wbx
title: Revise specscript-servers skill
status: completed
type: task
priority: normal
created_at: 2026-04-04T05:27:58Z
updated_at: 2026-04-04T05:43:02Z
---

Review and revise the specscript-servers SKILL.md after recent changes. Assess whether the skill is still needed.

## Work
- [x] Delete specscript-servers skill
- [x] Delete specscript-input-handling skill
- [x] Delete specscript-tests skill
- [x] Revise specscript-coding skill (packages, PWD, SQLite prepared statements, When command, Do command)

## Summary of Changes

- Deleted specscript-servers skill (stale, redundant)
- Deleted specscript-input-handling skill (code summary, not institutional knowledge)
- Deleted specscript-tests skill (tied to one sample project, covered by specs + coding skill)
- Revised specscript-coding skill:
  - Replaced GitHub URLs with local spec file paths
  - Added Packages and imports section
  - Added Connections section
  - Added When command (multi-branch conditional)
  - Added Do command (grouping commands)
  - Added Find command to data manipulation
  - Added SQLite section with prepared statements
  - Updated PWD variable description
