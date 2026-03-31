---
# specscript-q9kf
title: Move samples into specification directory
status: completed
type: feature
priority: normal
created_at: 2026-03-31T04:46:19Z
updated_at: 2026-03-31T04:51:03Z
---

Investigate and propose moving samples/basic and samples/http-server/sample-server into the specification directory to make it self-contained

## Investigation Tasks

- [x] Understand how cd= and SCRIPT_DIR work for relative paths
- [x] Check how sample-server is launched by the test harness
- [x] Check if Before tests can launch sample-server
- [x] Analyze which docs truly need shared sample files vs can inline
- [x] Determine best naming for the new location
- [x] Write proposal

## Summary of Changes

Updated plan/proposals/move-reference-samples.md with a comprehensive three-phase proposal based on thorough investigation of all concerns.
