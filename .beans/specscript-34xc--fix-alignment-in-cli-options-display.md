---
# specscript-34xc
title: Fix alignment in CLI options display
status: in-progress
type: bug
created_at: 2026-03-27T06:16:56Z
updated_at: 2026-03-27T06:16:56Z
---

toDisplayString() computes padding width from propertyName.length + 2 instead of the full formatted key (which includes the ', -x' short-option suffix). Keys like --output-json, -j overflow the width and descriptions don't align.
