---
# specscript-enl6
title: 'Bug: Print output missing from recursive sub-script calls'
status: completed
type: bug
priority: normal
created_at: 2026-03-27T07:07:57Z
updated_at: 2026-03-27T07:10:02Z
---

spec-ts hanoi sample exits correctly but doesn't print 'Move disc' lines. Kotlin prints them. Print inside Run script (recursive calls) doesn't produce console output.

## Summary of Changes

Removed setupSilentCapture() call from runScriptFile(). The child context shares the parent session by reference, so it already inherits the stdout writer. The silent capture was clobbering the shared session's stdout function, suppressing all Print output in sub-scripts.
