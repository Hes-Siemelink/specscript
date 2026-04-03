---
# specscript-dx2n
title: 'Document sample: Release folders README as reference template'
status: completed
type: feature
priority: normal
created_at: 2026-04-02T05:37:07Z
updated_at: 2026-04-02T07:19:33Z
---

Create a reference-grade README for the folders sample with testable examples and mock server setup. Template for all future samples.

## Plan

- [x] Phase 1: Record API responses from live Release server
- [x] Phase 2: Clean up recorded data (simplify IDs, meaningful names, minimal folders)
- [x] Phase 3: Create standalone mock server file in tests/
- [x] Phase 4: Rewrite README.md with documentation and CLI examples
- [x] Phase 5: Verify everything runs (tests pass, full build passes)
- [x] Phase 6: Create agent skill for sample READMEs

## Summary of Changes

- Recorded and cleaned API response data for folder listing (5 folders with meaningful names)
- Created standalone mock server (tests/mock-server.spec.yaml) on port 25102
- Created test infrastructure with connection overrides (tests/specscript-config.yaml)
- Created automated tests for list and move-by-id commands (tests/folder-tests.spec.yaml)
- Rewrote README.md with clear documentation, CLI examples, and file reference table
- Created specscript-sample-readme agent skill capturing the pattern
- Fixed wildcard endpoint bug: changed "*" to "{...}" (Ktor tailcard syntax) in both folders and cloud-connector tests
- Discovered executable README limitation: README.md can't easily override Connect to connections; tests live in tests/ directory instead
