---
# specscript-tvd7
title: TypeScript CLI implementation
status: completed
type: feature
priority: normal
created_at: 2026-03-27T06:29:56Z
updated_at: 2026-03-27T06:46:01Z
---

Build the CLI for TypeScript SpecScript: option parsing, directory invocation, script help, error reporting, test mode. 7-step plan in plan/proposals/typescript-cli.md.

## Tasks

- [x] Step 1: Bundling with tsup + spec-ts binary
- [x] Step 2: Option parsing and core CLI
- [x] Step 3: Script help and parameters
- [x] Step 4: Directory invocation
- [x] Step 5: Error reporting
- [x] Step 6: Consolidate cli-command.ts
- [x] Step 7: Test mode (--test)

## Summary of Changes

Full TypeScript CLI implementation matching Kotlin behavior:
- Option parsing state machine (splitArguments, parseCliOptions, resolveOption)
- Global options: --help, --output, --output-json, --debug, --test, --interactive (accepted/ignored)
- Usage banner with properly aligned options display
- Script help (--help): shows Script info description + Input schema/Input parameters options
- Directory invocation: description from specscript-config.yaml/README.md, command listing, hidden script filtering, recursive subcommand resolution
- Error reporting: CLI invocation errors, script errors, command errors, missing parameters
- Test mode (--test): YAML (Test case + Tests/Before/After), Markdown (Code example sections), directory recursive discovery, YAML report output
- Consolidated cli-command.ts to delegate to runCli via log callbacks (67 lines)
- Fixed Print output capture for in-process Cli command (threaded log callback through setupStdoutCapture)
- Fixed yaml CJS dynamic require issue (switched to ESM import for stringify)
- tsup bundling with yaml external, ESM format, node22 target
- 226 tests passing, 7 skipped (unchanged baseline)
