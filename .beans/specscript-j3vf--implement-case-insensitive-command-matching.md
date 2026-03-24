---
# specscript-j3vf
title: Implement case-insensitive command matching
status: completed
type: feature
priority: normal
created_at: 2026-03-24T07:28:13Z
updated_at: 2026-03-24T07:32:07Z
---

## Plan

- [x] Add canonicalCommandName() and commandEquals() to CommandHandler.kt
- [x] Use canonicalCommandName() in CommandLibrary.commandMap() for lowercase keys
- [x] Use canonicalCommandName() in FileContext.getCommandHandler(), addCommand(), getCliScriptFile()
- [x] Replace command.name == Handler.name comparisons in Script.kt with commandEquals()
- [x] Replace comparisons in TestUtil.kt with commandEquals()
- [x] Replace hardcoded strings in McpServer.kt with handler references + commandEquals()
- [x] Remove TODO comment from CommandLibrary.kt
- [x] Build and run all tests

## Summary of Changes

Implemented case-insensitive command matching across 6 files. Added canonicalCommandName() and commandEquals() to CommandHandler.kt, normalized all map keys and lookups in CommandLibrary and FileContext, replaced all direct string comparisons with commandEquals() in Script.kt, TestUtil.kt, and McpServer.kt. All 476+ tests pass with zero spec/sample changes.
