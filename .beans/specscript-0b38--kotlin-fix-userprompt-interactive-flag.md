---
# specscript-0b38
title: 'Kotlin: Fix UserPrompt interactive flag'
status: completed
type: task
priority: normal
created_at: 2026-03-30T20:26:49Z
updated_at: 2026-03-30T20:29:18Z
---

Pass interactive flag to UserPrompt.prompt() and UserPrompt.select() instead of using answers.isNotEmpty() as mode switch. Same pattern as the TypeScript fix.

## Summary of Changes

Fixed the prompt interactive flag in the Kotlin implementation:

- UserPrompt.kt: Both prompt() and select() now take an explicit interactive parameter. Resolution order: recorded answer -> default -> interactive prompt (if allowed) -> empty string/error. Eliminated the answers.isNotEmpty() mode-switch bug.
- ParameterDataPrompt.kt: All functions (prompt, promptText, promptBoolean, promptChoice, promptByType, promptObject, promptList) thread interactive through.
- Prompt.kt, Confirm.kt, PromptObject.kt: Pass context.interactive to prompt calls.
- InputParameters.kt: Passes interactive=true when context.interactive is true.
- Full build passes (unit tests + 440+ specification tests).
