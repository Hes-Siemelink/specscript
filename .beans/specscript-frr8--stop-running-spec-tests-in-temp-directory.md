---
# specscript-frr8
title: Stop running spec tests in temp directory
status: completed
type: task
priority: normal
created_at: 2026-04-04T05:33:37Z
updated_at: 2026-04-05T11:17:53Z
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

## Parked\n\nTestUtil no-temp-dir change parked on branch no-temp-dir-in-tests. Main keeps temp dir for now.


## Analysis: spec --test from non-project-root (2026-04-05)

### Empirical results

From project root: 37 failures (all HTTP/sample-server-related)
From /tmp: 45 failures (37 HTTP + 8 CWD-dependent)

### The 8 CWD-dependent failures

All come from .spec.yaml test files (NOT .spec.md Code Examples):

**File: specification/commands/core/files/tests/Locate files in the same way.spec.yaml** (5 tests)
- Read file from working directory (short way) - uses relative path from CWD
- Read file from working directory (file property) - uses relative path from CWD  
- Run from working directory (file property) - uses relative path from CWD
- Shell command from current directory (short way) - uses relative path from CWD
- Basic usage (Read file.spec.md) - hardcodes specification/commands/core/files/greeting.yaml

**File: specification/commands/core/files/tests/Read file tests.spec.yaml** (2 tests)
- Read file from disk - uses specification/commands/... relative path
- Read file with variable syntax and multiple documents - same

**File: specification/commands/core/shell/tests/Shell tests.spec.yaml** (1 test)
- Run shell script - uses specification/commands/... relative path

### Root cause

These tests deliberately test workingDir-relative resolution (Read file: file property, Shell without cd). The workingDir defaults to Path.of(".") in FileContext.kt:31, which is the JVM's CWD. This works when CWD is the project root but fails from anywhere else.

The .spec.md Code Example tests do NOT have this problem because the temp-dir hack makes scriptDir/tempDir self-contained, and all cli/shell blocks default to SCRIPT_HOME.

### Architecture insight

There are two separate concerns:
1. Code Examples in .spec.md files use the temp-dir hack (scriptDir == tempDir). These work from any CWD.
2. .spec.yaml test files use FileContext(file) directly, inheriting workingDir=Path.of("."). These 8 tests explicitly exercise CWD-relative resolution and REQUIRE CWD to be the project root.

### Fix options

Option A: Set workingDir to the project root in spec --test mode. Detect project root by walking up from the spec dir until finding a marker (build.gradle.kts, .git, etc). Fragile — ties tests to repo structure.

Option B: Set workingDir to the spec file's parent dir in getTestCases(). Then change the 8 tests to use relative paths from their own directory instead of from project root. This is more correct — tests should be self-contained.

Option C: Accept the status quo. These 8 tests intentionally test CWD-relative behavior. Running from project root is the expected contract, same as Gradle.



## Fix Plan: Make spec --test CWD-independent

- [x] Set workingDir to scriptDir in getTestCases() (TestUtil.kt)
- [x] Fix Locate files in the same way.spec.yaml (5 tests)
- [x] Fix Read file tests.spec.yaml (2 tests)
- [x] Fix Shell tests.spec.yaml (1 test)
- [x] Fix Read file.spec.md Basic usage example (uses CWD-relative path)
- [x] Verify: spec --test from /tmp matches spec --test from project root

## Verification Results

spec --test produces identical results regardless of CWD:
- From project root: 475 passed, 50 failed
- From /tmp: 475 passed, 50 failed
- diff of failure lists: empty (identical)

The 50 pre-existing failures are:
- 37 HTTP ConnectException (mock server not started in spec --test mode)
- 13 SQLite native lib failures (platform issue)

None of these are CWD-related.

## TypeScript Implementation Complete (2026-04-05)

All 3 TypeScript fixes applied and verified:

1. **WriteFile (files.ts)**: Resolves filename against context.workingDir
2. **Eager SCRIPT_TEMP_DIR (context.ts)**: Created in constructor; tempDir getter reads from variables map (allowing test runner override); single cleanup listener pattern avoids MaxListenersExceeded warning
3. **Test runner (spec-runner.test.ts)**: Set workingDir to dirname(fullPath) for .spec.yaml tests (matching Kotlin's scriptDir approach)

Results: 485 passed, 0 failed (was 21 failures before). The fixes also resolved 14 pre-existing SQLite/Store/Save test failures.
