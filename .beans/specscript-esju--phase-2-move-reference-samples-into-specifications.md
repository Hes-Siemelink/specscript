---
# specscript-esju
title: 'Phase 2: Move reference samples into specification/code-examples/'
status: completed
type: task
priority: normal
created_at: 2026-03-31T05:38:41Z
updated_at: 2026-03-31T05:44:17Z
---

git mv samples/basic, samples/hello.spec.yaml, and samples/http-server/sample-server into specification/code-examples/.
Update all spec files, test infrastructure, and documentation that reference these paths.

## Tasks\n\n- [x] Create specification/code-examples/ directory with specscript-config.yaml (hidden: true)\n- [x] git mv samples/basic specification/code-examples/basic\n- [x] git mv samples/hello.spec.yaml specification/hello-world.spec.yaml\n- [x] git mv samples/http-server/sample-server specification/code-examples/sample-server\n- [x] Update Running SpecScript files.spec.md (cd=samples → cd=specification/code-examples)\n- [x] Update Command line options.spec.md (cd=samples → cd=specification/code-examples)\n- [x] Update SpecScriptTestSuite.kt (SAMPLE_SERVER path)\n- [x] Update TypeScript test runner (sample server path)\n- [x] Update AGENTS.md (update samples/basic sensitive area path)\n- [x] Update README.md, README-old.md, README-2.md, CLAUDE.md, typescript/README.md\n- [x] Run full test suite (Kotlin: all pass)\n- [x] Verify TypeScript tests (2 pre-existing failures from Phase 1, filed as specscript-km2e)

## Summary of Changes\n\nMoved reference samples into specification/code-examples/:\n- samples/basic/ → specification/code-examples/basic/\n- samples/hello.spec.yaml → specification/hello-world.spec.yaml\n- samples/http-server/sample-server/ → specification/code-examples/sample-server/\n\nUpdated all references in spec files, test infrastructure, READMEs, and AGENTS.md.\nAll Kotlin tests pass (507 tests). Two TypeScript failures are pre-existing (filed as specscript-km2e).
