---
# specscript-gd1h
title: Connection system overhaul (Phases 1-4)
status: completed
type: epic
priority: high
created_at: 2026-03-21T08:54:36Z
updated_at: 2026-03-21T13:08:17Z
---

Implement the 4-phase connection system improvement plan:
- Phase 1: SCRIPT_DIR variable (independent)
- Phase 2: ${env} namespace (independent)  
- Phase 3: env: property on Input schema (depends on Phase 2)
- Phase 4: Connection inheritance / caller wins (independent)

Proposals confirmed. Draft specs in plan/draft-specs/. Next: finalize specs in specification/ dir, then implement.

## Summary

All 4 phases completed:
- Phase 1: SCRIPT_DIR variable (specscript-8g4l)
- Phase 2: env namespace (specscript-1dhm)
- Phase 3: env on Input schema (specscript-lbx3)
- Phase 4: Connection inheritance (specscript-ho63)
