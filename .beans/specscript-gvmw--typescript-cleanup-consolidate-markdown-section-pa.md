---
# specscript-gvmw
title: 'TypeScript cleanup: consolidate markdown section parsing, Phase 3'
status: completed
type: task
priority: normal
created_at: 2026-07-14T06:05:18Z
updated_at: 2026-07-14T06:08:16Z
---

Phase 3 of plan/proposals/typescript-cleanup.md, scoped down: extract shared parseMarkdownScripts(content) helper (scanMarkdown + splitMarkdownSections + filter empty sections) used by all 4 call sites (cli.ts executeFile, cli.ts runMarkdownTests, commands/run.ts runMarkdownScript, test/spec-runner.test.ts runMarkdownFile). Per-caller reset/execution semantics (test isolation vs continuous run) are intentionally different and stay as-is -- only the identical scan+split+filter boilerplate is unified.

## Summary of Changes

Added parseMarkdownScripts(content): Script[] to src/markdown/converter.ts (scanMarkdown + splitMarkdownSections + filter empty sections in one place). Updated all 4 call sites to use it, dropping their duplicated scan/split/filter boilerplate: cli.ts executeFile, cli.ts runMarkdownTests, commands/run.ts runMarkdownScript, test/spec-runner.test.ts runMarkdownFile.

Scoped down from the proposal's literal suggestion of one shared execution helper: the 3 non-Run.ts callers have genuinely different reset semantics between sections (executeFile resets only capturedOutput for output grouping during continuous execution; the two test runners reset error+input+capturedOutput to isolate each section as an independent test; run.ts's nested Run: file: execution resets nothing since sections continue as one script). Merging that logic would either lose test isolation or continuous-execution semantics, so only the identical parsing step was unified -- the true duplication.

Verified: full test suite 539 passed / 7 skipped (unchanged). Manually exercised all 3 production code paths beyond the test suite: plain .spec.md execution, --test mode on a .spec.md file, and a hand-written nested Run: file: pointing at a multi-section .spec.md (confirms commands/run.ts's runMarkdownScript still runs sections in sequence on a shared context).
