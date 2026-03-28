# Spec-Level Alignment Analysis

How well do the specification documents align with the language levels system, and what should change?

## Current State

`specification/levels.yaml` assigns spec files across 8 levels (0–6, with two Level 6 modules). A cross-reference
of every file against every command it uses revealed 29 instances of cross-level contamination across 18 files.

Most contamination has been addressed. This document tracks the remaining open items.

## What Was Done

- **On error and On error type moved from Level 1 to Level 0** — error handling is inseparable from Error.
- **Contaminated test cases cleaned up** — thin implementation tests (SCRIPT_HOME vs SCRIPT_TEMP_DIR, Add schema
  validation) removed; For each tests using Read file extracted to `For each tests - L3.spec.yaml`; Schema tests
  reassigned to Level 5.
- **skip-sections added to levels.yaml** for 11 spec.md files with mixed-level sections.
- **Before all tests example rewritten** — new L0 example using variables; HTTP example kept as secondary section.
- **Answers example rewritten** — new L0 example using Input parameters; Prompt example kept as secondary section.
- **Kernel concept documented** in levels.yaml header (Levels 0+1 form the kernel).
- **Dependency graph added** to levels.yaml header, replacing inconsistent `requires` fields. MCP corrected from
  `requires: [4]` to `requires: [1]`; HTTP `requires` removed (cumulative from L0+L1).
- **Assignment registry mapping** documented as comment.
- **ParameterData.schema.yaml** added to tracked shared schemas.

## Remaining Recommendations

### ~~J. Make command handler traits explicit in the spec~~ — DONE

All 77 command spec files updated: Content type → Input, Value → Scalar, implicit → auto-iterate.
Raw input row added to 23 DelayedResolver commands. Error trap row added to 3 ErrorHandler commands.
Rows omitted when `no` (default).

### M. Consider making levels.yaml a proper spec

Right now levels.yaml is a plain YAML file with no validation. It could become a SpecScript spec file
(`Language levels.spec.md`) with executable tests that verify:

- Every spec file on disk appears in exactly one level
- Every registered command appears in exactly one level
- `skip-sections` entries reference sections that actually exist in the file

This would catch drift automatically as part of `./gradlew specificationTest`. However, this is a Level 3+ capability
(needs file system access to scan directories), so it can't self-test at lower levels. A unit test may be more practical.
