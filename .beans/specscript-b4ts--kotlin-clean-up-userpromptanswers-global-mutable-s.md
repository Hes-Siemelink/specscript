---
# specscript-b4ts
title: 'Kotlin: Clean up UserPrompt/Answers global mutable state'
status: completed
type: task
priority: low
created_at: 2026-03-27T11:43:44Z
updated_at: 2026-03-27T12:00:31Z
---

Refactor Kotlin's global mutable UserPrompt.default singleton and Answers global state to use context-based dispatch instead. The TypeScript implementation already uses context-based answers (stored in session map), which is cleaner. This is a code quality improvement with no functional change.

## Approach

Pass answers map (Map<String, JsonNode>) through prompt functions instead of using global Answers.recordedAnswers. Answers command stores into context.session. UserPrompt dispatch checks the answers map to decide between test simulation and real prompting. Removes UserPrompt.default global var and TestPrompt/KInquirerPrompt swap.

## Tasks

- [x] Refactor UserPrompt: add answers parameter to prompt() and select()
- [x] Refactor ParameterDataPrompt: thread answers through all extension functions
- [x] Refactor Answers command: store in context.session instead of global map
- [x] Refactor Prompt, PromptObject, Confirm: pass answers from session
- [x] Refactor InputParameters: pass answers from session
- [x] Remove TestPrompt as separate object, fold into UserPrompt dispatch
- [x] Remove UserPrompt.default global var
- [x] Remove UserPrompt.default = TestPrompt from test setup
- [x] Run full build (./gradlew build)

## Summary of Changes

Refactored global mutable UserPrompt.default and Answers.recordedAnswers singletons to use context-based dispatch. Answers are now stored in context.session instead of a global map. UserPrompt dispatches between real KInquirer prompts and test simulation based on an answers parameter. TestPrompt was folded into UserPrompt. The Cli command now propagates the parent session to child CLI invocations for answer propagation. All 483 spec tests and unit tests pass.
