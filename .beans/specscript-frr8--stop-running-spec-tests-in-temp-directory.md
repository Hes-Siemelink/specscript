---
# specscript-frr8
title: Stop running spec tests in temp directory
status: in-progress
type: task
priority: normal
created_at: 2026-04-04T05:33:37Z
updated_at: 2026-04-05T05:49:17Z
---

Disable the temp directory for markdown spec tests. Instead of creating a temp directory and setting it as both scriptDir and tempDir, run tests in the spec file's own directory. The user will fix failing docs/tests case-by-case.

## Failing Tests (19 of 533)

### Pattern 1: temp-file blocks create files, then referenced via resource/bare name (15 tests)
These fail because temp-file writes to tempDir but scriptDir is now the real directory, so file resolution doesn't find them.

- [ ] `SpecScript Yaml Scripts.spec.md` > Script output
- [ ] `Variables.spec.md` > Script output
- [ ] `Variables.spec.md` > Script Input & Output
- [ ] `Testing.spec.md` > Testing
- [ ] `Organizing SpecScript files in directories.spec.md` > Calling another script
- [ ] `Input parameters.spec.md` > Using types
- [ ] `Validate schema.spec.md` > Schema from file
- [ ] `Create credentials.spec.md` > Basic usage
- [ ] `Connect to.spec.md` > Connection inheritance
- [ ] `Http server.spec.md` > Running a script file
- [ ] `SpecScript files as commands.spec.md` > Basic usage
- [ ] `Read file.spec.md` > Reading temp files created in Markdown
- [ ] `Run script.spec.md` > Basic usage
- [ ] `Run script.spec.md` > Passing input parameters
- [ ] `Run script.spec.md` > Finding the script
- [ ] `Temp file.spec.md` > Resolve variables
- [ ] `Mcp server.spec.md` > Deriving metadata from script
- [ ] `Mcp server.spec.md` > Tools as a list of scripts
- [ ] `Mcp tool.spec.md` > Deriving metadata from script

## Implementation Plan

- [ ] Create Cd command (changes context.workingDir)\n- [ ] Create schema file\n- [ ] Make workingDir mutable (var) in ScriptContext and FileContext
- [ ] Make workingDir mutable (var) in ScriptContext and FileContext  
- [ ] Register Cd in CommandLibrary
- [ ] Inject Cd command in Script.kt for yaml specscript cd=dir blocks
- [ ] Revert TestUtil temp directory removal (keep running in temp dir for now)

## Previous Implementation

cd= attribute approach was attempted and reverted — it introduced hacks into the core execution loop. See plan/proposals/spec-tests-without-temp-directory.md for findings and alternative approaches.

## Changes Made

### TestUtil.kt (two changes)

1. **Added URIs to DynamicContainers** (line 75): Changed `dynamicContainer(it.name, tests)` to `dynamicContainer(it.name, it.toUri(), tests.stream())`. This gives IntelliJ the file/directory source it needs for navigation and rerun.

2. **Removed temp directory for markdown tests** (lines 166-177): `getCodeExamplesAsTests()` now uses `FileContext(file)` directly instead of creating a temp directory. Temp files are still written to a lazily-created tempDir via the TempFile command.

## IntelliJ Integration Investigation

See plan/reports/intellij-test-integration.md for full findings. The issue remains unresolved. Most promising hypothesis: move TestUtil.kt back to the specificationTest source set so JUnit deps are test-scoped, matching the working instacli setup.

## Proposal

The cd= attribute approach was reverted. A proposal for an official Run in command has been written at plan/proposals/run-in-command.md. This makes the directory-scoping a first-class language feature instead of a test infrastructure hack.

## Progress

Cd command implemented. Schema, command class, CommandLibrary registration, ScriptContext var change — all done. All tests pass (specification + unit).

Remaining: inject Cd in Script.kt for yaml specscript cd=dir blocks, revert TestUtil.
