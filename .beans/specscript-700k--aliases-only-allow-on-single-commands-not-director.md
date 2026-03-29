---
# specscript-700k
title: 'Aliases: only allow on single commands, not directories'
status: completed
type: task
priority: normal
created_at: 2026-03-29T13:17:38Z
updated_at: 2026-03-29T14:22:34Z
parent: specscript-krm1
---

Currently aliases work on directory imports (as: formal), which registers all commands with the alias as a prefix (e.g. 'Formal hi'). This forces prefix conventions into SpecScript code. Change to only allow aliasing on single command imports — renaming one command, not prefixing a batch. Update spec, Kotlin, and TypeScript.\n\n## Todo\n\n- [x] Update spec: alias example now shows single command (sub/hi as: greet)\n- [x] Update Kotlin: aliases on Command and Name items, removed from Directory\n- [x] Update TypeScript: same changes as Kotlin\n- [x] Run all tests (Kotlin: check passes, TypeScript: 412/412)

## Summary of Changes

Changed alias support from directories to single commands. Previously, aliasing a directory (as: formal) prefixed all commands with the alias. Now, aliases work only on individual command imports, renaming one command at a time.

Files changed:
- specification/language/Packages.spec.md: Alias example now shows sub/hi as: greet
- PackageImport.kt: alias field moved from Directory to Command and Name; new parseWithAlias method
- PackageRegistry.kt: addDirectoryCommands no longer accepts alias; Command/Name scanning uses alias
- package-import.ts: Same type changes as Kotlin
- package-registry.ts: Same scanning changes as Kotlin
