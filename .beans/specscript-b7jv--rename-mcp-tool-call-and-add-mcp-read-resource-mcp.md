---
# specscript-b7jv
title: Rename Mcp tool call and add Mcp read resource, Mcp get prompt
status: completed
type: feature
priority: normal
created_at: 2026-04-03T14:23:22Z
updated_at: 2026-04-03T16:56:35Z
---

Rename Mcp tool call to Mcp call tool for consistent verb-after-Mcp pattern. Add Mcp read resource and Mcp get prompt commands. Move test cases to dedicated test file in tests/ directory.

## Plan\n\n- [x] Write proposal\n- [x] Confirm proposal\n- [x] Write/update specs\n- [x] Confirm specs\n- [x] Implement Kotlin\n- [x] Implement TypeScript\n- [x] Create tests/Mcp client tests.spec.yaml with all edge cases\n- [x] Run all tests (520 tests, 0 failures)\n- [x] Update samples

## Report

### Test results
- Kotlin: 520 spec tests, 0 failures
- TypeScript: 460 tests, 0 failures, 13 skipped (pre-existing skips unrelated to this work)

### What was done
Three MCP client commands now exist: `Mcp call tool` (renamed from `Mcp tool call`), `Mcp read resource`, `Mcp get prompt`. SSE transport removed from both server and client sides. 13 edge-case tests moved to `tests/Mcp client tests.spec.yaml` with shared server setup.

### Design decisions
- **`input` not `arguments`** for client commands — aligns with SpecScript's `Input schema` / `${input.x}` vocabulary. Server-side prompt definitions keep `arguments` to match the MCP protocol's own terminology. This is an intentional inconsistency: we follow whichever convention is native to each context.
- **No deprecation period for the rename** — `Mcp tool call` is gone. The `⚠️` marker on the commit signals the breaking change.
- **SSE removed entirely** — deprecated by the MCP spec itself. The existing SSE test was already broken (FIXME). No point maintaining dead transport code.
- **`parseMcpTextContent()` in TypeScript** — the JS `yaml` library parses strings containing colons (like `def foo(): pass`) as YAML maps. The Kotlin Jackson parser doesn't. Added a targeted helper that only attempts YAML parsing on multi-line text, since `resultToString` returns plain strings for scalars. This avoids modifying the generic `parseYamlIfPossible` which would break HTTP client tests.

### Things the reviewer should know
- Port 8091 is shared across three spec.md files (one server per file, started/stopped within each). Port 8098 is used exclusively by the test file.
- The `server` property block is duplicated across all three client command schemas. A shared `$ref` could eliminate this, but that's a separate concern.
- The `Mcp server.spec.md` diff is mostly deletions (SSE section, verbose HTTP bullet list). The remaining content is cleaner.
