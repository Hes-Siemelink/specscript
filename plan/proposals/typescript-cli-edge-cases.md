# TypeScript CLI — Edge Cases and Spec Gaps

Cases discovered by cross-referencing the specification with the Kotlin implementation.

Priority:
- **Fix now** — spec tests depend on it, or it's trivial to get right inline during the step it belongs to
- **Defer** — no spec test coverage, defensive/cosmetic, or fix-when-it-breaks

---

## Fix Now

### Usage banner text

The exact text is tested character-for-character in `Cli.spec.md`. Already hardcoded in `cli-command.ts` — move to
shared code during consolidation. See "Usage banner options" below for approaches to keep it maintainable.

### Default output is YAML

Kotlin defaults to YAML output even without `-o`. The spec states this explicitly. Multiple spec examples depend on it.

### Command name derivation from filenames with spaces

`Cli.spec.md` directory listing shows `multiple-choice` — the hyphenation must be correct. Already implemented in
`cli-command.ts`, needs to move to shared code.

### Description from README.md

The `samples/basic` listing in `Cli.spec.md` shows "Simple SpecScript example scripts" from `README.md`. Without this,
the directory listing test fails. Already implemented in `cli-command.ts`.

### `Cli` command `cd` option

Tested directly in `Cli.spec.md` "Cli in a different directory". Already works — ensure it survives consolidation.

### Short options

`-h`, `-o`, `-j`, `-d`, `-t`, `-i` — trivial to support during option parsing. Would be surprising not to.

### Output of null/undefined

Don't print anything when output is null/undefined. One conditional. Kotlin's `printOutput()` checks `isNotBlank()`.

### Empty directory → "No commands available."

Tested by `Script info.spec.md` "Hidden commands" (hidden file → empty listing). Already handled in `cli-command.ts`.

### `spec --help` (no file) same as `spec` (no args)

Both produce the usage banner. Tested by both `Running SpecScript files.spec.md` and `Cli.spec.md`.

---

## Defer

### Subdirectories in command list

Kotlin lists subdirectories alongside spec files. No spec test covers a mixed directory. Match Kotlin behavior when
convenient, but don't over-engineer — `Cli.spec.md` tests `samples/basic` which has only spec files.

### specscript-config.yaml `Script info` field

Kotlin reads `Script info:` from config YAML. No CLI test validates it. Support it when implementing directory info,
but don't stress about it.

### Invalid global option error message

`spec --bogus` → "Invalid option: bogus". No spec test. Implement if it falls out naturally from option parsing.

### Combined short options

`-oj` is NOT supported (Kotlin strips dashes and looks up `oj`, which fails). Don't support. Match Kotlin.

### YAML output formatting

Kotlin uses `MINIMIZE_QUOTES`. The TS `yaml` library may differ. Test and adjust if a spec test fails, don't
pre-optimize.

### JSON output formatting

`JSON.stringify(value, null, 2)` should match Kotlin's `toDisplayJson()`. Verify when implementing `--output-json`.

### Print vs Output printing

Already clear from the architecture. `Print` writes to stdout during execution; CLI prints `${output}` after. No
special handling needed.

### Input parameters vs Input schema

Both define inputs. Both are tested in their respective spec files. Support both when implementing `--help`, but
`Input parameters` is deprecated — `Input schema` is the primary path.

### Help on scripts without Script info

No spec test. Handle gracefully — print nothing.

### All test mode edge cases

Report format, field order, recursive directory discovery, empty report, failure format — all untested by spec.
Implement to match Kotlin when building test mode (step 7), but these are internal details.

### Debug/error format details

Node.js stacks differ from Java. No spec test can validate TS-specific traces. Match the structure
("Scripting error", "Caused by:", "In <file>:") but don't try to match Java class names.

### Error context display

Kotlin prints the failing command as YAML. May need enhanced error metadata. Address when implementing error reporting.

### File not found message

"Could not find spec file for: X" — no spec test. Implement naturally.

---

## Usage banner options

The banner is tested character-by-character in `Cli.spec.md`. Three approaches to handle this:

**A. Generate from YAML definition.** Read `specscript-command-line-options.yaml` at build/startup time and format the
banner programmatically — same approach as Kotlin. The banner stays in sync with the option definitions. Risk: the
formatting (column widths, spacing) must match exactly, so the generator itself becomes a source of subtle bugs.

**B. Keep it hardcoded.** The banner text rarely changes (6 options, unlikely to grow). Hardcode it in one place (shared
module), reference from both `cli.ts` and `cli-command.ts`. Simple, no generation logic. Risk: if someone adds an
option to the YAML and forgets the banner, they drift — but the `Cli.spec.md` test would catch it.

**C. Single source in the spec, test drives the code.** The banner text lives in `Cli.spec.md` as the authoritative
source. Both Kotlin and TS hardcode it; the spec test is the enforcement mechanism that catches drift. This is what
already happens today — the spec test IS the single source of truth. No code change needed, just acknowledge the
pattern.
