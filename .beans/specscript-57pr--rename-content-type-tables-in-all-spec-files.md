---
# specscript-57pr
title: Rename Content type tables in all spec files
status: completed
type: task
priority: normal
created_at: 2026-03-28T07:37:00Z
updated_at: 2026-03-28T07:40:52Z
---

Update all command spec files: rename Content type→Input, Value→Scalar, implicit→auto-iterate. Add Raw input and Error trap rows for commands that have DelayedResolver or ErrorHandler traits.

## Summary of Changes\n\nUpdated all 77 command spec files:\n- Renamed table: Content type → Input, Value → Scalar, implicit → auto-iterate\n- Added Raw input row to 23 commands implementing DelayedResolver\n- Added Error trap row to 3 commands implementing ErrorHandler (On error, On error type, Expected error)\n- Fixed 4 inconsistent Yes → yes capitalizations\n- All specification tests pass
