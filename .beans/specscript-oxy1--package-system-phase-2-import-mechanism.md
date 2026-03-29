---
# specscript-oxy1
title: 'Package system Phase 2: Import mechanism'
status: scrapped
type: task
priority: normal
created_at: 2026-03-28T20:45:53Z
updated_at: 2026-03-29T14:08:16Z
parent: specscript-iglq
---

Implement the imports section in specscript-config.yaml with compact map syntax: directory imports, specific command imports, aliased imports, import all, and collision detection.

## Todo

- [ ] Parse new compact map-based imports syntax in DirectoryInfo
- [ ] Implement directory import resolution (non-recursive)
- [ ] Implement specific command import resolution
- [ ] Implement aliased imports (as keyword)
- [ ] Implement import all
- [ ] Implement collision detection with error messages naming both sources
- [ ] Wire imported package commands into FileContext as priority 4
- [ ] Write unit tests for imports
- [ ] Verify draft spec import tests pass

## Reasons for Scrapping\n\nThis work was completed as part of specscript-krm1 (Package system v2 implementation). Import mechanism with compact map syntax, aliased imports, collision detection, and FileContext wiring were all implemented and committed in earlier commits.
