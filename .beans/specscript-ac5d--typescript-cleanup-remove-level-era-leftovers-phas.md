---
# specscript-ac5d
title: 'TypeScript cleanup: remove Level-era leftovers, Phase 1'
status: completed
type: task
priority: normal
created_at: 2026-07-14T05:54:59Z
updated_at: 2026-07-14T05:58:34Z
---

Low-risk cleanup from plan/proposals/typescript-cleanup.md Phase 1: remove Level references, dead skippedBlocks field, unused imports, fragile Node cast, and vague error message. No behavioral change.

- [x] Remove skippedBlocks field from Script class
- [x] Remove skippedBlocks check in spec-runner.test.ts
- [x] Remove Level comments in converter.ts
- [x] Remove Level section comments in register.ts
- [x] Delete registerLevel0Commands() in register.ts
- [x] Update registerAllCommands() JSDoc
- [x] Delete unused JsonObject import in command-handler.ts
- [x] Delete unused JsonObject/isObject imports in command-execution.ts (also dropped unused CommandFormatError on same line)
- [x] Drop parentPath fallback cast in spec-runner.test.ts
- [x] Rewrite error message at context.ts:396, add setRunFileFn comment
- [x] Run tests, then report

## Summary of Changes

Removed all Level-era leftovers and small dead code across 7 files: skippedBlocks field/plumbing (Script, converter.ts, spec-runner.test.ts), Level N section comments (register.ts, converter.ts), the unused registerLevel0Commands() deprecated wrapper, the stale Node parentPath fallback cast, unused imports (JsonObject in command-handler.ts and command-execution.ts, plus CommandFormatError found unused on the same line), and the misleading Level 3 reference in context.ts's error message (also added a comment explaining the setRunFileFn circular-dep workaround). No behavioral change. Test suite: 539 passed, 7 skipped (unchanged from baseline). tsc --noEmit shows only pre-existing errors unrelated to these files.

Note: a broader sweep with tsc --noUnusedLocals found many more unused imports/locals across the codebase (cli.ts, mcp-server.ts, commands/files.ts, commands/http.ts, commands/shell.ts, commands/testing.ts, language/conditions.ts, language/eval.ts, language/script.ts, markdown/converter.ts, util/yaml.ts, etc.) — out of scope for this Phase 1, left as a finding for a future cleanup pass.
