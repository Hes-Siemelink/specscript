---
name: specscript-new-command
description: Step-by-step guide for adding a new command to SpecScript. Covers specification, schema design, Kotlin implementation, and testing phases. Use when creating a new SpecScript command.
compatibility: Requires the specscript project structure and Gradle build.
metadata:
  author: specscript
  version: "1.0"
---

## Overview

Adding a new command to SpecScript follows a 4-phase process: specification, technical design, Kotlin implementation,
and testing. Always follow the spec-first development process defined in AGENTS.md — write the spec before implementing.

## Phase 1: Specification

1. Start with the simplest, most natural way a user would express their intent
2. Write a `.spec.md` file with realistic, declarative examples
3. Expand to cover edge cases and complex scenarios
4. Place the spec in the appropriate `specification/commands/` subdirectory

Load the `specscript-specs` skill for details on code block types and specification document rules.

## Phase 2: Technical design

1. Study similar commands in `src/main/kotlin/specscript/commands/` for structural consistency
2. Design the YAML structure and determine content type support:
   - **Value**: simple scalar content (string, number, boolean)
   - **List**: array content
   - **Object**: map/dictionary content
3. Create `schema/CommandName.schema.yaml` — the JSON Schema definition for the command's YAML structure

## Phase 3: Kotlin implementation

1. Create a `CommandHandler` singleton in `src/main/kotlin/specscript/commands/`
   - Implement `ObjectHandler` if the command takes object content
   - Implement `DelayedResolver` if the command needs raw YAML without variable expansion
   - Other interfaces as needed (study existing commands)
2. Register the constructor with name and group: `CommandHandler("Command name", "group/subgroup")`
3. Implement the `execute` method with the command logic
4. Register the command in `CommandLibrary.kt`
5. Reuse existing classes and utilities — good OO/encapsulation
6. Use `ScriptContext.session` for cross-command state when appropriate

## Phase 4: Testing

1. Rapid iteration (skip tests during build): `./gradlew build fatJar -x test -x specificationTest`
2. Validate individual spec: `spec your-spec.spec.md`
3. Full suite: `./gradlew specificationTest`
