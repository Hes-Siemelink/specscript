---
# specscript-2x0q
title: 'TypeScript cleanup: unify test-format handling, Phase 2'
status: completed
type: task
priority: normal
created_at: 2026-07-14T06:02:31Z
updated_at: 2026-07-14T06:03:46Z
---

Phase 2 of plan/proposals/typescript-cleanup.md: extract a shared runScriptTest helper in test/spec-runner.test.ts used by both Tests: (runStructuredTests) and legacy Test case: (runFlatTests) branches, closing the gap where SKIP_TESTS was not honored for legacy Test case: tests. Script.splitTests()/splitTestCases() stay as-is (Option A: both formats remain valid).

## Summary of Changes

Extracted registerScriptTest(name, script, createContext) in test/spec-runner.test.ts, used by both runStructuredTests and runFlatTests to register a vitest it() with SKIP_TESTS handling and silent capture applied consistently. Verified no currently-passing .spec.yaml Test case: test relies on a SKIP_TESTS entry (the only matching entries belong to two files already fully excluded via SKIP_FILES), so this is behavior-preserving today while closing the gap for future legacy tests. cli.ts's runYamlTests was reviewed and left untouched -- it already converges both formats onto one shared runSingleTest helper, no duplication to unify there. Test suite: 539 passed, 7 skipped (unchanged).
