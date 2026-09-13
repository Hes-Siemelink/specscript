# Report: CWD-independent spec tests

Bean: specscript-frr8

## Test results

- **Kotlin**: 547 specification tests pass, 0 failures. Identical results from project root and `/tmp`.
- **TypeScript**: 485 passed, 13 skipped, 0 failed. Previously had 21 pre-existing failures (SQLite, Store, Save as) which are now fixed as a side effect.

## Problem

Running `spec --test` from any directory other than the project root produced different results. 8 `.spec.yaml` test files used paths relative to the JVM/Node CWD, so they only worked from the project root.

Additionally, `Write file` in both implementations resolved filenames against the process CWD rather than `context.workingDir`, which is incorrect.

## Fix

Three changes, applied to both Kotlin and TypeScript:

1. **Test runner sets `workingDir` to the script's parent directory** for `.spec.yaml` test files. Previously Kotlin used `Path.of(".")` (CWD) and TypeScript used `SPECSCRIPT_HOME` (project root). Now both use `scriptDir`, making tests self-contained.

2. **`Write file` resolves against `context.workingDir`** instead of the process CWD. Bug existed in both implementations.

3. **`SCRIPT_TEMP_DIR` created eagerly** in the context constructor. Previously it was lazy, which meant `${SCRIPT_TEMP_DIR}` in YAML variable references failed with "Unknown variable" because variables are read from the map, not the property getter.

## Spec file changes

8 test files updated to use paths relative to their own directory instead of the project root:

- `Locate files in the same way.spec.yaml` (5 tests) — relative paths to sibling files
- `Read file tests.spec.yaml` (2 tests) — same
- `Shell tests.spec.yaml` (1 test) — same
- 3 SQLite/Store test files — `out/sample.db` changed to `${SCRIPT_TEMP_DIR}/sample.db`
- `Save as tests.spec.yaml` — uses `${SCRIPT_TEMP_DIR}`
- `Write file.spec.md` — uses `${SCRIPT_TEMP_DIR}/greeting.txt`
- `Read file.spec.md` — removed CWD-relative path from basic usage example

## Architecture notes

The `.spec.md` Code Examples already used a temp-dir hack (`scriptDir == tempDir`) and were CWD-independent. Only `.spec.yaml` test files had the CWD dependency.

In TypeScript, the eager `SCRIPT_TEMP_DIR` required the `tempDir` getter to always read from the variables map rather than caching in a field. The test runner overrides `SCRIPT_TEMP_DIR` after construction (to align with the temp-dir hack for Code Examples), so the getter must respect that override.

Temp directory cleanup uses a shutdown hook with `deleteRecursively()` (Kotlin) / `rmSync` with a single process exit listener (TypeScript) to avoid both leaked directories and `MaxListenersExceeded` warnings.

## What to look for in the diff

- `TestUtil.kt` line setting `workingDir = scriptDir` — the core Kotlin fix
- `spec-runner.test.ts` changing `SPECSCRIPT_HOME` to `dirname(fullPath)` — the core TypeScript fix
- `WriteFile.kt` and `files.ts` — the `workingDir` resolution fix
- `FileContext.kt` and `context.ts` — eager `SCRIPT_TEMP_DIR` and cleanup
