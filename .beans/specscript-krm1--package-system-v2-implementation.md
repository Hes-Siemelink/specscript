---
# specscript-krm1
title: Package system v2 implementation
status: completed
type: feature
priority: normal
created_at: 2026-03-29T08:03:02Z
updated_at: 2026-03-29T14:29:25Z
---

Implement revised package system per Packages-2 proposal. No inline FQNs, YAML map alias syntax, local imports with ./ prefix, package imports via search path.

## Todo

- [x] Write executable spec: specification/language/Packages.spec.md
- [~] Implement PackageInfo and PackageImport data classes in DirectoryInfo.kt
- [x] Implement PackageRegistry (search path, package scanning, excluded dirs)
- [x] Implement local imports (./ prefix) in DirectoryInfo parsing
- [x] Implement import resolution in FileContext (package + local imports)
- [x] Implement alias syntax (YAML map with as:)
- [x] Remove old file-path import support
- [x] Remove inline FQN resolution from command resolution order
- [x] Migrate existing configs (goals-app, digitalai, core/files/tests)
- [x] Update Organizing SpecScript files in directories.spec.md
- [x] Run full test suite

## Summary of Changes\n\nAll child tasks completed: slash notation, directory-based package names, TypeScript port, aliases restricted to single commands, self-package discovery, bare directory imports, and ../ cleanup. Package system v2 is feature-complete.
