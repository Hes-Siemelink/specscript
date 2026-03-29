---
# specscript-1ruy
title: 'Import syntax: dot notation to slash notation'
status: completed
type: task
priority: normal
created_at: 2026-03-29T13:05:44Z
updated_at: 2026-03-29T13:58:44Z
parent: specscript-krm1
---

Change specific command imports from dot notation (greetings.hi) to slash notation (greetings/hi). Slash is consistent with wildcard syntax (greetings/*, greetings/**) and reflects that these are directory paths, not Java-style namespaces. Removes dot ambiguity. Update spec examples, Kotlin parseElement/parseImportString, and TypeScript parseImportString.

## Summary of Changes\n\n- Import syntax changed from dot notation (sub.hi) to slash notation (sub/hi)\n- Kotlin: PackageImport.kt parse() uses lastIndexOf('/') instead of lastIndexOf('.')\n- TypeScript: package-import.ts parseImportString() uses lastIndexOf('/') instead of lastIndexOf('.')\n- Wildcard patterns (sub/*, sub/**) already used slash notation and were unaffected
