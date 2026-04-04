---
# specscript-hvrq
title: Run command (replaces Run script)
status: completed
type: feature
priority: normal
created_at: 2026-04-04T07:24:45Z
updated_at: 2026-04-04T08:21:46Z
---

Rename Run script to Run and extend with cd, inline script, and file properties. Unifies script execution patterns. See plan/proposals/run-in-command.md

## Plan

- [x] Write proposal (plan/proposals/run-in-command.md)
- [x] Write draft specs (plan/draft-specs/)
- [x] User review of draft specs
- [x] Write Run.schema.yaml in specification/
- [x] Implement Run command (rename + extend RunScript.kt)
- [x] Implement command delegation for inline script form
- [x] Update existing Run script.spec.md or replace with Run.spec.md
- [x] Add Running SpecScript from SpecScript.spec.md to specification/language/
- [x] Add Run script alias in CommandLibrary
- [x] Add resource as deprecated alias for script
- [x] Run tests (554 passing, 0 failing)
- [x] Update bean, report, commit

## Summary of Changes

New `Run` command that replaces `Run script` as the primary script execution command.

**Implementation:**
- `Run.kt` — new command with value form, `script` (string/inline), `file`, `cd`, `input` support
- `RunScript.kt` — refactored to thin alias delegating to `Run`
- `FileContext.kt` — added `parentCommandLookup` for inline script command delegation
- `CommandLibrary.kt` — registered both `Run` and `Run script`

**Specification:**
- `specification/commands/core/files/Run.spec.md` — command reference
- `specification/commands/core/files/schema/Run.schema.yaml` — JSON schema
- `specification/commands/core/files/tests/Run tests.spec.yaml` — 12 test cases
- `specification/language/Running SpecScript from SpecScript.spec.md` — isolation ladder doc

## Phase 2: Remove Run script\n\n- [x] Delete RunScript.kt\n- [x] Remove RunScript from CommandLibrary.kt\n- [x] Delete Run script.spec.md, Run script.schema.yaml, Run script tests.spec.yaml\n- [x] Remove alias section from Run.spec.md and alias test from Run tests.spec.yaml\n- [x] Update all spec files using Run script: to use Run:\n- [x] Update levels.yaml, README.md, overview docs\n- [x] Update sample files\n- [x] Run tests — all passing



## Phase 3: TypeScript Port

- [x] Create typescript/src/commands/run.ts with full Run command (cd, inline script, file, parentCommandLookup, delayedResolver)
- [x] Delete typescript/src/commands/run-script.ts
- [x] Update typescript/src/language/context.ts (parentCommandLookup, setRunFileFn, parent delegation)
- [x] Update typescript/src/commands/register.ts (import/registration)
- [x] Update typescript/test/spec-runner.test.ts (file references)
- [x] All 481 TypeScript tests passing
