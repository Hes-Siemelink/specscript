---
# specscript-huim
title: 'TS: Unknown command ''Get all credentials'' - command name resolution bug'
status: completed
type: bug
priority: normal
created_at: 2026-04-01T18:37:18Z
updated_at: 2026-04-01T18:40:41Z
---

TypeScript implementation fails with 'Unknown command: Get all credentials' on a spec.yaml file that Kotlin handles fine. Need to investigate how command name resolution differs between implementations and whether spec coverage is missing.

## Investigation Findings

### Root Cause
`Get all credentials` is a built-in command in Kotlin (Level 5 connections subsystem) that has NOT been implemented in TypeScript. This is expected — the TypeScript implementation doesn't yet have the connections subsystem (tracked by bean specscript-5ww1).

### Is test coverage missing?
No. The connections commands are Level 5 in levels.yaml, and the TypeScript test runner only includes tests up to Level 4. So the spec tests correctly don't attempt to run connection command tests. The HIGHER_LEVEL_COMMANDS skip set in the test runner does only list 'Connect to' and 'Credentials' (not 'Get all credentials', 'Set default credentials', etc.), but this doesn't matter because those test files aren't even included in the test lists.

### The user's scenario
The user ran a sample file (samples/digitalai/release/credentials/select-default.spec.yaml) directly with spec-ts. This sample uses Get all credentials and Set default credentials — both unimplemented in TypeScript. This is not a spec gap — it's expected behavior for an unimplemented feature level.

### Verdict
NOT a bug in the spec or the TypeScript command resolution logic. The connections subsystem simply isn't implemented yet in TypeScript.
