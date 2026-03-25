---
# specscript-4dc0
title: TypeScript Level 0 implementation
status: in-progress
type: feature
priority: normal
created_at: 2026-03-25T20:52:08Z
updated_at: 2026-03-25T20:59:36Z
---

Implement the SpecScript TypeScript runtime covering Level 0: core YAML-to-command pipeline, variable resolution, conditions, 21 commands, CLI entry point, and spec test runner.

## Todo

- [x] Scaffold project: package.json, tsconfig.json, vitest.config.ts, directory structure
- [x] Install dependencies (pnpm)
- [x] Core types (types.ts): JsonValue, Command, error hierarchy
- [x] YAML utilities (yaml.ts): multi-doc parsing, display formatting
- [x] Command handler interface and registry (command-handler.ts)
- [x] Script context interface and default implementation (context.ts)
- [x] Variable resolution (variables.ts)
- [x] Conditions system (conditions.ts)
- [x] Command execution pipeline (command-execution.ts)
- [x] Script parsing and execution (script.ts)
- [x] Level 0 commands: Print, Output, As, Assignment, Do, Exit, Error
- [ ] Level 0 commands: Script info, Input parameters, Input schema
- [x] Level 0 commands: Assert equals, Assert that, Expected output, Expected console output, Expected error
- [x] Level 0 commands: Test case, Code example, Answers, Tests, Before all tests, After all tests
- [x] CLI entry point (cli.ts)
- [x] Update .gitignore with Node exclusions
- [x] Spec test runner (test/spec-runner.ts)
- [ ] Run Level 0 spec tests and fix failures
