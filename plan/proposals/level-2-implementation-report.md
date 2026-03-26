# Level 2 Implementation Report

Findings from the TypeScript Level 2 implementation (113/138 tests passing, 25 expected skips).

## Test Results

| Suite | Passed | Skipped | Failed |
|---|---|---|---|
| Level 0 | 37 | 1 | 0 |
| Level 1 | 64 | 3 | 0 |
| Level 2 | 12 | 21 | 0 |
| **Total** | **113** | **25** | **0** |

Level 2 skips (21 tests) are sections that use L3+ commands:
- Prompt (5 sections) — needs Level 3
- Read file (4 sections) — needs Level 3
- Shell/ShellBlock (4 sections) — needs Level 3
- ShellCli (4 sections) — needs Level 3
- YamlFile (3 sections) — needs Level 3
- Best Practices (1 file) — no executable sections at any level

## Scope

Level 2 adds Markdown document parsing: scanning `.spec.md` files into typed blocks, converting
blocks to executable commands, splitting by headers into test sections, and running them against
the existing command set.

Total new code: ~350 lines (120 scanner + 141 converter + ~90 test runner additions).
Modified: `Script` class (added `skippedBlocks` field).

## 1. Scanner state machine: the empty lastLinePrefix bug

**The critical bug.** The scanner is a line-by-line state machine that classifies Markdown content
into typed blocks. Each block type has a `firstLinePrefix` (to detect the start) and a
`lastLinePrefix` (to detect the end).

Header blocks have `lastLinePrefix: ''` (empty string). In Kotlin, `"anything".startsWith("")`
returns `true`, so after a Header block is created, the very next line triggers the end-of-block
check and returns to Text. Header blocks are single-line by design — the header text is stored
in `headerLine`, not in `lines`.

In the initial TypeScript implementation, the end-of-block check was:

```typescript
if (currentBlock.type.lastLinePrefix && line.startsWith(currentBlock.type.lastLinePrefix))
```

The `&&` guard short-circuits because `''` is falsy in JavaScript. The end-of-block check never
fires for Header blocks, so every subsequent line accumulates inside the Header. The entire
document after the first `#` becomes a single Header block.

**Fix:** Remove the truthy guard. The check becomes `line.startsWith(currentBlock.type.lastLinePrefix)`,
matching Kotlin exactly. Since `''.startsWith('')` is `true` in JavaScript too, Header blocks
end immediately — correct behavior.

**For the Go implementer:** `strings.HasPrefix("anything", "")` returns `true` in Go, so this
will work naturally. Do not add a guard for empty prefixes.

## 2. Stdout isolation between shared-context sections

Each `.spec.md` file runs all its sections in a shared context (matching Kotlin). The shared
context preserves variables and state across sections — this is by design, as later sections
can reference variables set in earlier ones.

In Kotlin, `Script.run()` wraps execution in `ExpectedConsoleOutput.captureSystemOutAndErr()`,
which redirects `System.out` to a fresh `ByteArrayOutputStream` for each run. After the run,
the stream is removed from the session. So each section starts with an empty stdout buffer.

In the TypeScript implementation, stdout is captured via a persistent array on
`context.session['capturedOutput']`. Since the context is shared, Print output from one section
persists into the next. If section A prints "Hello" and doesn't assert, section B sees "Hello"
in its `Expected console output`.

**Fix:** Reset the captured output array before each section in the test runner:

```typescript
const captured = sharedContext.session.get('capturedOutput') as string[] | undefined
if (captured) captured.length = 0
```

This matches Kotlin's behavior where each `Script.run()` starts with a fresh capture stream.

**For the Go implementer:** Whatever stdout capture mechanism you use, ensure it resets between
sections in a shared-context markdown test run. The Kotlin architecture does this implicitly
through `captureSystemOutAndErr`; other implementations need to do it explicitly.

## 3. Skipping sections with unavailable commands

The `SpecScript Markdown Documents.spec.md` file is self-testing — it describes all Markdown
constructs and demonstrates them with live examples. Many sections use commands from Level 3+
(Shell, Cli, Temp file, Read file, Prompt). These sections must be skipped gracefully.

Two detection mechanisms:
1. **Command-level:** After converting blocks to a Script, check if any command name is
   unregistered. This catches YAML-embedded commands like `Prompt` and `Read file`.
2. **Block-level:** Track block types that were skipped during conversion (Shell, ShellCli,
   YamlFile). If a section had these blocks, the `Expected console output` commands in that
   section depend on output that was never produced — so the section should be skipped.

The `Script` class gained a `skippedBlocks: string[]` field to carry this information from
the converter to the test runner. This is TS-specific scaffolding; when all levels are
implemented, no sections are skipped and the field is always empty.

## 4. Empty test suite handling

`SpecScript Best Practices.spec.md` contains no executable code blocks — it's pure prose. The
scanner finds no SpecScriptYaml/HiddenSpecScriptYaml/Output/Quote blocks, so all sections
produce empty scripts, which are filtered out. Vitest throws an error for empty `describe()`
blocks ("No test found in suite").

**Fix:** Add a placeholder `it.skip('no executable sections at this level')` when no sections
produce runnable tests.

## 5. Block type priority ordering

The scanner checks block types in priority order. Two ordering constraints:

- `YamlFile` (````yaml file`) must come before `SpecScriptYaml` (````yaml specscript`) —
  both start with ` ```yaml`.
- `ShellCli` (````shell cli`) must come before `ShellBlock` (````shell`) — both start
  with ` ```shell`.

This matches Kotlin's `blockTypes` list (lines 40-49 of `SpecScriptMarkdown.kt`). Getting the
order wrong causes `yaml file` blocks to be parsed as `yaml specscript` (producing garbage
commands) or `shell cli` blocks to be parsed as `shell` (losing the cli semantics).

**For the Go implementer:** Use an ordered slice for block type detection, not a map. The first
match wins.

## 6. Quote block handling

Quote blocks (`> text`) are special: they're detected by a prefix on every line, not by an
opening/closing pair. The scanner handles them as a special case before the main block type
dispatch. When a `> ` line appears while in Text mode, a Quote block is started. When a
non-`> ` line appears while in Quote mode, the scanner falls through to the normal
block-end/block-start logic.

Quote blocks are converted to `Print` commands. This is the Markdown equivalent of writing
`Print: text` in YAML.

## Files added/modified

**New files:**
- `src/markdown/scanner.ts` — Markdown scanner, block types, `scanMarkdown()` (120 lines)
- `src/markdown/converter.ts` — `blocksToScript()`, `splitMarkdownSections()`, `getTestTitle()` (141 lines)

**Modified files:**
- `src/language/script.ts` — added `skippedBlocks` field to Script class
- `test/spec-runner.test.ts` — Level 2 test files, `runSpecMdFile()`, command availability check, skipped blocks check, empty suite handling

## Summary of recommendations

### For the Go implementer

1. Do not guard the block-end check for empty `lastLinePrefix` — let `HasPrefix(line, "")` return true naturally (§1)
2. Reset stdout capture between sections in shared-context markdown tests (§2)
3. Use an ordered slice for block type priority, not a map (§5)

### For the SpecScript maintainer

1. The `SpecScript Markdown Documents.spec.md` file is the most complex test file so far — it
   tests the Markdown parser by being a Markdown file itself. Consider adding more focused test
   cases in `tests/SpecScript Markdown tests.spec.md` for edge cases (nested headers, empty
   blocks, consecutive block types).
