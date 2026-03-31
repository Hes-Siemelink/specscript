---
# specscript-7uqm
title: Fix failing Organizing spec test
status: completed
type: bug
priority: normal
created_at: 2026-03-31T05:27:32Z
updated_at: 2026-03-31T05:28:08Z
---

The specscript-config.yaml in my-scripts/ has a Script info description that overrides the README.md description, causing --help output mismatch. Remove the description from config, keep only hidden: true.

## Summary of Changes\n\nRemoved Script info description from specification/language/my-scripts/specscript-config.yaml so that README.md provides the directory description ('A collection of simple scripts') as expected by the spec test. The config now only contains 'hidden: true'.
