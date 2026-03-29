---
# specscript-n715
title: 'TypeScript: Port package system'
status: completed
type: task
priority: normal
created_at: 2026-03-29T12:07:00Z
updated_at: 2026-03-29T12:20:41Z
parent: specscript-krm1
---

Port the package system (import parsing, PackageRegistry, CLI flag) to the TypeScript implementation. 7 phases per the plan in plan/proposals/typescript-package-system.md.

## Plan

- [x] Phase 1: Fix 5 help output failures (CLI help alignment + --package-path flag)
- [x] Phase 2: Implement import parsing (PackageImport, ImportItem, parseImports)
- [x] Phase 3: Implement PackageRegistry (package discovery, command scanning)
- [x] Phase 4: Integrate into DefaultContext (replace findImportedCommand)
- [x] Phase 5: CLI flag (--package-path / -p sets PackageRegistry.packagePath)
- [x] Phase 6: Update SKIP_TESTS, add Packages spec to test runner
- [x] Phase 7: Run full test suite, verify all tests pass

## Summary of Changes

Ported the package system from Kotlin to TypeScript across 7 phases:

- Added --package-path/-p CLI flag with value-bearing option support
- Created package-import.ts (PackageImport, ImportItem types, parseImports)
- Created package-registry.ts (package discovery, command scanning, local imports)
- Replaced findImportedCommand in DefaultContext with lazy resolveImportedCommands using the new package system
- Fixed CLI command default working directory (workingDir -> tempDir) to match Kotlin
- Fixed package path resolution (relative paths resolved against CLI working directory)
- Added Packages.spec.md to test runner (10 tests), unskipped 3 previously-blocked tests
- Final: 410 tests passing, 13 skipped (was 389 passing, 8 failing, 16 skipped)
