---
# specscript-xvke
title: 'Package info: derive name from directory, drop name field'
status: completed
type: task
priority: normal
created_at: 2026-03-29T12:43:30Z
updated_at: 2026-03-29T13:58:39Z
parent: specscript-krm1
---

Package info should work like Script info — presence marks a directory as a package, directory name IS the package name. The spec already states this ('A directory becomes a package by declaring Package info'). Just need to: remove name field from the YAML example, update Kotlin/TypeScript to derive package name from directory name instead of reading it from config, support string shorthand (Package info: description) and object form with description field.

## Summary of Changes\n\n- PackageInfo now accepts string shorthand (Package info: Description text)\n- Package name is derived from the directory name, not from a name field\n- isPackage() checks for presence of Package info, not name match\n- Updated on-disk lib/greetings/specscript-config.yaml to match spec\n- Changes applied to both Kotlin and TypeScript implementations
