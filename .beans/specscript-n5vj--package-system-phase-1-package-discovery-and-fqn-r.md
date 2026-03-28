---
# specscript-n5vj
title: 'Package system Phase 1: Package discovery and FQN resolution'
status: todo
type: task
priority: normal
created_at: 2026-03-28T20:45:52Z
updated_at: 2026-03-28T20:46:04Z
parent: specscript-iglq
---

Implement package discovery (search path, Package info parsing, excluded directories) and FQN command resolution. Core infrastructure for the package system.

## Todo

- [ ] Create PackageInfo data class and parse Package info from specscript-config.yaml
- [ ] Create PackageRegistry: search path resolution, package discovery, caching
- [ ] Implement excluded directory logic (tests/, hidden: true)
- [ ] Implement FQN parsing and command resolution (dot-notation to package/directory/command)
- [ ] Wire FQN resolution into FileContext.getCommandHandler() as priority 5
- [ ] Write unit tests for package discovery and FQN resolution
- [ ] Verify draft spec FQN tests pass
