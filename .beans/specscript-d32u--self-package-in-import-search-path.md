---
# specscript-d32u
title: Self-package in import search path
status: completed
type: task
priority: normal
created_at: 2026-03-29T13:37:13Z
updated_at: 2026-03-29T14:19:48Z
parent: specscript-krm1
---

The package containing the current script should be automatically included in the import search path. This means scripts within a package can import from sibling directories using package imports instead of upward local imports (../). Eliminates most ../ use cases: tests/ importing from goals/ becomes a package import rather than ./../goals. Local ./ imports remain for simple cases where no package declaration exists.\n\n## Todo\n\n- [x] Write proposal (plan/proposals/self-package-search-path.md)\n- [x] Write spec additions (Packages.spec.md: Enclosing package discovery section)\n- [x] Implement in Kotlin (PackageRegistry.autoPackagePath + findEnclosingPackageLibrary)\n- [x] Implement in TypeScript (package-registry.ts + context.ts)\n- [x] Run all tests (Kotlin: check passes, TypeScript: 412/412)

## Summary of Changes

Added auto-discovery of the enclosing package library directory to the search path. When a script is inside a package, the parent directory of that package is automatically added as the highest-priority entry in the package search path. This lets scripts within a package import from their own package and sibling packages without --package-path.

Files changed:
- specification/language/Packages.spec.md: New Enclosing package discovery section with executable test
- PackageRegistry.kt: Added autoPackagePath field and findEnclosingPackageLibrary method
- FileContext.kt: Set autoPackagePath in init block
- package-registry.ts: Added autoPackagePath, setAutoPackagePath, findEnclosingPackageLibrary
- context.ts: Set autoPackagePath in DefaultContext constructor
