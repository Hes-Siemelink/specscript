---
# specscript-tu5o
title: 'TypeScript: Fix prompt interactive flag'
status: completed
type: task
priority: normal
created_at: 2026-03-30T20:23:28Z
updated_at: 2026-03-30T20:25:35Z
---

Update promptSelect to use interactive parameter (matching promptText), and update all callers to pass context.interactive

## Summary of Changes

Fixed the prompt interactive flag in the TypeScript implementation:

- `user-prompt.ts`: Both `promptText` and `promptSelect` now take an explicit `interactive` parameter instead of using `answers.size > 0` as a mode switch
- `prompt.ts`: All callers (`doPrompt`, `promptChoice`, `promptByType`, `promptBoolean`, `promptObjectProperties`) thread `context.interactive` through
- `confirm.ts`: Updated `promptSelect` call to pass `context.interactive`
- All 413 TypeScript spec tests pass
- No new TypeScript compilation errors
