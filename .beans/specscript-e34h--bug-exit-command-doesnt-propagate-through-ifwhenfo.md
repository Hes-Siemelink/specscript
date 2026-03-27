---
# specscript-e34h
title: 'Bug: Exit command doesn''t propagate through If/When/ForEach branches'
status: completed
type: bug
priority: normal
created_at: 2026-03-27T06:56:38Z
updated_at: 2026-03-27T07:02:08Z
---

runBranch calls script.run() which catches Break. Should call script.runCommands() to let Break propagate. Hanoi sample hangs due to infinite recursion.

## Tasks
- [x] Add spec test for Exit inside If/then branch
- [x] Fix Do.execute: run -> runCommands
- [x] Fix runBranch: run -> runCommands
- [x] Fix ForEach body: run -> runCommands
- [x] Fix Repeat body: run -> runCommands
- [x] Fix runErrorHandling: run -> runCommands
- [x] Run tests (227 passing, +1 from new Exit test)
- [x] Rebuild and verify hanoi sample (no longer hangs)

## Summary of Changes

Fixed Exit command propagation through control flow branches (If/When/Do/ForEach/Repeat/OnError). Changed script.run() to script.runCommands() in 5 locations so Break exceptions propagate instead of being caught. Also changed runCommands() to return JsonValue (matching Kotlin) so ForEach/Do can use the return value. Added spec test for Exit inside If branch. Hanoi sample no longer hangs.
