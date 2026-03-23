---
# specscript-y49t
title: 'Bug fix: ParameterData boolean deserialization'
status: completed
type: bug
priority: normal
created_at: 2026-03-23T20:22:21Z
updated_at: 2026-03-23T20:24:37Z
---

ParameterData lacks a boolean JsonCreator constructor, causing MismatchedInputException when YAML contains boolean values in parameter positions (e.g. optional: true as a shorthand).

- [x] Analyze the bug path
- [x] Create a test case
- [x] Fix ParameterData
- [x] Build and verify

## Summary of Changes

The bug was in samples/digitalai/release/folders/flat-folder-list.spec.yaml where hidden: true was incorrectly indented as a sibling of Input schema properties instead of being under Script info. Also added a boolean JsonCreator to ParameterData for resilience.
