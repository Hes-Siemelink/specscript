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

### J. Make command handler traits explicit in the spec

Every command spec already has a "Content type / Supported" table documenting which YAML shapes the command accepts
(Value, List, Object). This covers `ValueHandler`, `ListHandler`, and `ObjectHandler`. But two critical traits are
missing from the specs entirely:

- **DelayedResolver** (19 commands) — the command receives raw YAML before variable resolution; it controls when and
  whether `${...}` expressions are evaluated. Essential for control flow (`If`, `For each`, `When`), error handling
  (`On error`), and structural commands (`Tests`, `As`, `Http server`, all MCP commands).

- **ErrorHandler** (2 commands: `On error`, `On error type`) — the command catches errors from subsequent commands
  rather than propagating them.

These traits fundamentally change how the engine dispatches a command. The TypeScript port had to reverse-engineer them
from Kotlin source — the spec documents don't mention them.

**Fix**: Add these traits to each command's spec file by extending the Content type table with additional rows.

The current table also has naming issues. The names are implementation jargon — "Content type" is vague, "implicit"
is mysterious, and "DelayedResolver" is a Kotlin interface name. Proposed renamings:

| Current name     | Proposed name        | Why |
|------------------|----------------------|-----|
| Content type     | **Input**            | The table describes what shape of input the command accepts |
| Value            | **Scalar**           | "Value" is overloaded everywhere; "Scalar" is unambiguous (string, number, boolean) |
| List             | **List**             | Fine as-is |
| Object           | **Object**           | Fine as-is |
| implicit         | **auto-iterate**     | Says what actually happens: the engine iterates over list items and calls the command once per item |
| DelayedResolver  | **Raw input**        | The command receives its input before variable resolution — it gets the raw YAML |
| ErrorHandler     | **Error trap**       | The command catches errors from subsequent commands |

Result for `If`:

```markdown
| Input      | Supported     |
|------------|---------------|
| Scalar     | no            |
| List       | auto-iterate  |
| Object     | yes           |
| Raw input  | yes           |
| Error trap | no            |
```

Result for `On error`:

```markdown
| Input      | Supported     |
|------------|---------------|
| Scalar     | no            |
| List       | auto-iterate  |
| Object     | yes           |
| Raw input  | yes           |
| Error trap | yes           |
```

Result for `Print` (simple command, all defaults):

```markdown
| Input      | Supported     |
|------------|---------------|
| Scalar     | yes           |
| List       | yes           |
| Object     | yes           |
```

For simple commands where Raw input and Error trap are both `no`, those rows can be omitted — they default to `no`.
This keeps the table compact for the ~50 commands that don't use either trait, while making the ~19 delayed resolvers
and 2 error handlers immediately visible.

### M. Consider making levels.yaml a proper spec

Right now levels.yaml is a plain YAML file with no validation. It could become a SpecScript spec file
(`Language levels.spec.md`) with executable tests that verify:

- Every spec file on disk appears in exactly one level
- Every registered command appears in exactly one level
- `skip-sections` entries reference sections that actually exist in the file

This would catch drift automatically as part of `./gradlew specificationTest`. However, this is a Level 3+ capability
(needs file system access to scan directories), so it can't self-test at lower levels. A unit test may be more practical.
