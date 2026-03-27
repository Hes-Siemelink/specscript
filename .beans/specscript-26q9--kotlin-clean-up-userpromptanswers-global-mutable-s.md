---
# specscript-26q9
title: 'Kotlin: Clean up UserPrompt/Answers global mutable state'
status: todo
type: task
priority: low
created_at: 2026-03-27T11:08:54Z
updated_at: 2026-03-27T11:08:54Z
---

Refactor Kotlin prompt architecture to eliminate global mutable state. Thread UserPrompt through ScriptContext instead of UserPrompt.default var. Store answers in context instead of Answers.recordedAnswers global map. Extract TestPrompt and KInquirerPrompt into separate files. Move renderInput helpers to TestPrompt file. Delete dead code (InputParameters.handleInputType, commented lines in UserInteraction.kt). See plan/proposals/typescript-prompt-interactive.md 'Kotlin Cleanup Opportunity' section. Main branch work.
