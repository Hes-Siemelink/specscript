---
# specscript-34xc
title: Fix alignment in CLI options display
status: completed
type: bug
priority: normal
created_at: 2026-03-27T06:16:56Z
updated_at: 2026-03-27T06:29:10Z
---

toDisplayString() computes padding width from propertyName.length + 2 instead of the full formatted key (which includes the ', -x' short-option suffix). Keys like --output-json, -j overflow the width and descriptions don't align.

## Summary of Changes\n\nFixed toDisplayString() width calculation in ObjectProperties.kt to use full formatted key length instead of propertyName.length + 2. Updated 4 spec files, README, and .gitignore.
