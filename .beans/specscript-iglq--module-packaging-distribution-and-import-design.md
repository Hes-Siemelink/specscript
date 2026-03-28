---
# specscript-iglq
title: Module packaging, distribution and import design
status: in-progress
type: feature
priority: normal
created_at: 2026-03-28T18:55:42Z
updated_at: 2026-03-28T20:46:09Z
---

Design proposal for Tier 2 module packaging, distribution, and import. Covers namespacing, packaging format, and import mechanism.

## Todo

- [x] Research current import mechanism and file organization
- [x] Analyze design space (Unix PATH, Java packages, TypeScript modules)
- [x] Write proposal with recommendation
- [x] Reconcile with hierarchical digitalai example\n- [x] Revise proposal with dot-notation and hierarchical modules\n- [x] Apply user feedback: tests/ exclusion, drop relative imports, resolve open questions\n- [x] User approved proposal\n- [x] Create implementation plan\n- [x] Write draft spec: Modules.spec.md (with real sample files in specification/language/module-samples/)\n- [x] Review draft spec and sample files with user\n- [x] Drop export control (hidden: true is sufficient)\n- [x] Switch to compact map-based import syntax\n- [ ] Phase 1: Package discovery and FQN resolution (specscript-n5vj)\n- [ ] Phase 2: Import mechanism (specscript-oxy1)\n- [ ] Phase 3: Migration and spec cleanup (specscript-ax6e)
