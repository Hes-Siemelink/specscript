---
# specscript-074e
title: 'Local imports: bare directory reference implies wildcard'
status: completed
type: task
priority: normal
created_at: 2026-03-29T13:29:18Z
updated_at: 2026-03-29T14:29:13Z
parent: specscript-krm1
blocked_by:
    - specscript-jalv
---

Allow ./utils without items to mean 'import all commands from that directory', equivalent to ./utils: "*". Reduces ceremony for the common case. Needs a spec example and implementation in both Kotlin and TypeScript parseImports (handle null/empty value for a local source).

## Summary of Changes\n\nAdded support for bare source references in imports (greetings: with no value). This is equivalent to greetings: "*" (import all root-level commands). Works for both package imports and local imports.\n\nFiles changed:\n- specification/language/Packages.spec.md: Added bare reference section with test\n- PackageImport.kt: Handle null node in parseList as wildcard\n- package-import.ts: Handle null/undefined in parseImportItems as wildcard
