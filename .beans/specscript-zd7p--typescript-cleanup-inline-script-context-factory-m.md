---
# specscript-zd7p
title: 'TypeScript cleanup: inline script context factory + misc, Phase 4-5'
status: completed
type: task
priority: normal
created_at: 2026-07-14T06:09:47Z
updated_at: 2026-07-14T06:13:31Z
---

Phase 4 of plan/proposals/typescript-cleanup.md: add DefaultContext.createInlineChildContext() factory method, replacing runInlineScript's manual construct-then-mutate pattern in commands/run.ts. Also Phase 5b (hide stripBlockScalarNewlines param behind a markdown-specific entry point) and 5c (move deleteOnShutdown/temp-dir cleanup out of context.ts into util/). 5a (circular-dep comment) already done in Phase 1.

## Summary of Changes

Phase 4: Added DefaultContext.createInlineChildContext(cdPath?) in context.ts, lifting runInlineScript's construct-then-wipe-then-mutate logic (fresh variables, SCRIPT_HOME/PWD/env/input, parentCommandLookup wiring) into one method. commands/run.ts's runInlineScript is now 4 lines. Preserved the exact existing behavior including the pre-existing quirk of passing the directory as scriptFile (not a real file path) -- did not attempt to fix it, out of scope for a refactor.

Phase 5b: util/yaml.ts's parseYamlCommands(content) no longer takes a boolean flag. Added parseMarkdownYamlCommands(content) as the Markdown-specific entry point (internally both call a private parseYamlCommandsWithOptions). Updated markdown/converter.ts's one caller.

Phase 5c: Moved temp-dir creation and the process.on('exit') best-effort cleanup out of context.ts into a new util/temp-dir.ts (createTempDir()). context.ts no longer imports mkdtempSync/rmSync/tmpdir.

Verified: full suite 539 passed / 7 skipped (unchanged), tsc --noEmit shows the same 8 pre-existing errors (none new). Manually exercised: Run: script: inline with variable isolation + parentCommandLookup to local file commands + cd override; Markdown block-scalar newline stripping via --test; SCRIPT_TEMP_DIR creation and confirmed removal after process exit.
