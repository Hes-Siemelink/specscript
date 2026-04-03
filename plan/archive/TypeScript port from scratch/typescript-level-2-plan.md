# TypeScript Implementation Plan — Level 2: Markdown Documents

## Scope

Level 2 adds the Markdown document scanner — the ability to parse `.spec.md` files and extract executable code blocks.
No new commands; this is a language feature that makes SpecScript self-documenting.

**Exit criteria:**

- Markdown scanner parses `.spec.md` files into typed blocks
- Block-to-Script conversion generates executable commands from blocks
- Section splitting by `#` headers produces individual test cases
- `pnpm test` runs Level 0 + Level 1 + Level 2 spec tests and they pass

## What Level 2 Adds

### Markdown Block Types

| Block type | Markdown directive | Becomes command | Level available |
|---|---|---|---|
| SpecScriptYaml | ` ```yaml specscript` | YAML command list | 2 |
| HiddenSpecScriptYaml | `<!-- yaml specscript` | YAML command list | 2 |
| Answers | `<!-- answers` | Answers command | 2 |
| Output | ` ```output` | Expected console output | 2 |
| Quote | `> ` prefix | Print command | 2 |
| Header | `#` prefix | Section boundary (test case) | 2 |
| YamlFile | ` ```yaml file=` | Temp file command | 3 |
| ShellBlock | ` ```shell` | Shell command | 3 |
| ShellCli | ` ```shell cli` | Cli command | 3 |

At Level 2, YamlFile/ShellBlock/ShellCli are recognized by the scanner but NOT converted to commands (those commands
don't exist yet). Sections containing only Level 3+ blocks will produce empty scripts and be skipped.

### Scanning Algorithm

Line-by-line state machine (matching Kotlin exactly):

1. Start in `Text` mode
2. Quote (`> `) is a special case: checked first, creates a Quote block that continues while lines start with `> `
3. When in Text mode: check if line matches any block's `firstLinePrefix` (in priority order). If yes, start that block.
4. When in a non-Text block: check if line starts with the block's `lastLinePrefix`. If yes, end block and return to Text.
5. Otherwise: add line to current block.

Priority order for block detection: YamlFile, HiddenSpecScriptYaml, SpecScriptYaml, ShellCli, ShellBlock, Answers, Output, Header, Text.

### Section Splitting

Each `#` heading starts a new section. Sections with no executable blocks are skipped. Each section's title comes from
either the heading text or the first `Code example:` command.

## Files

- `src/markdown/scanner.ts` — NEW: MarkdownScanner, BlockType, MarkdownBlock
- `src/markdown/converter.ts` — NEW: blocksToScript(), splitMarkdownSections()
- `test/spec-runner.test.ts` — MODIFIED: add Level 2 test suite for .spec.md files

## Test Files

- `language/SpecScript Markdown Documents.spec.md` — main spec (partial: L3+ sections will fail)
- `language/tests/SpecScript Markdown tests.spec.md` — additional tests (pure L2)
- `language/SpecScript Best Practices.spec.md` — no executable blocks (will produce 0 tests)
