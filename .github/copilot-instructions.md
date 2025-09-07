---
description: AI rules derived by SpecStory from the project AI interaction history
globs: *
---

## Headers

## TECH STACK

## PROJECT DOCUMENTATION & CONTEXT SYSTEM

## CODING STANDARDS

## WORKFLOW & RELEASE RULES

## DEBUGGING

## REFACTORING RULES & GUIDELINES
- When refactoring, follow a multi-phase approach, ensuring each phase is completed and tested before proceeding.
- During refactoring, the AI assistant should read the log file (`.claude/log.txt`) to understand the current state and continue from where it left off.
- When providing options during refactoring, clearly state the options and the decision made. In the case of dependency management, prefer using thin JARs and let Gradle handle transitive dependencies (Option 1).
- After removing implementation directories during refactoring, update the build configuration to depend on the new library, create a minimal CLI bootstrap, and test the setup.
- When working in a git-controlled directory (e.g., a dedicated branch for refactoring), avoid creating unnecessary backup files, as git serves as the backup system.
- When refactoring and there is a naming conflict (e.g., main functions in different projects), rename one of the conflicting elements to resolve the conflict.