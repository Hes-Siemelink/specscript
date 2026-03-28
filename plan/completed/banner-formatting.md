# Banner and Formatted Output: Cross-Implementation Strategy

## Problem

The SpecScript usage banner and help output are tested character-for-character via `Expected console output`. This
creates tension when a second implementation (TypeScript) must produce identical output:

1. **The formatting is accidental.** The Kotlin `toDisplayString()` algorithm uses `max(propertyName.length) + 2` as the
   padding width, which does not account for the `, -x` short-option suffix. Keys like `--output-json, -j` (17 chars)
   overflow the width (13) and get only the 3-space minimum gap, while shorter keys get extra padding. The descriptions
   don't align. This is not a deliberate design — it's an artifact of the algorithm.

2. **TypeScript must reverse-engineer it.** Generating from `specscript-command-line-options.yaml` is the right
   approach (option A from the edge cases doc), but the generator must replicate Kotlin's specific algorithm, including
   its quirks. Any deviation — even an improvement — breaks the spec test.

3. **This problem gets worse.** Interactive prompts, directory listings, script help, and error messages all produce
   formatted text. Each is a new surface where Kotlin's formatting choices become de-facto spec.

## Analysis

There are four dimensions to consider:

### Dimension 1: Source of truth for the option definitions

Not controversial. `specscript-command-line-options.yaml` is the single source. Both implementations read it and
generate output. This is option A from the edge cases doc.

### Dimension 2: Source of truth for the formatting algorithm

This is the actual tension. Three sub-options:

**2a. Kotlin's algorithm is the spec.** TypeScript replicates `infoString()` and `toDisplayString()` exactly. The
YAML definitions file plus the algorithm together produce the banner. The spec test is a regression check, not a
definition.

**2b. The spec test output IS the spec.** The algorithm is an implementation detail. Each implementation is free to use
any algorithm that produces the exact output shown in the spec. Since the spec output was pasted from Kotlin, this is
equivalent to 2a in practice, but philosophically different: the spec is the authority, not the code.

**2c. The spec defines structure, not whitespace.** Allow formatting divergence between implementations. The spec would
need a less strict assertion mechanism.

**Recommendation: 2a.** It's the simplest. The Kotlin algorithm is 15 lines of code. Replicating it in TypeScript is
trivial and eliminates all ambiguity. The fact that the alignment is imperfect is a known wart — fixing it means
changing the Kotlin algorithm, the spec, and the TypeScript port simultaneously, which is a separate concern.

### Dimension 3: Asserting formatted output across implementations

The current `Expected console output` does `actual.trim() === expected.trim()` — exact match. Options:

**3a. Keep exact matching, live with it.** The spec shows what users actually see. When formatting changes, update the
spec. This is already the workflow for Kotlin. TypeScript joins the same workflow.

**3b. Add a "structural" assertion.** Something like `Expected console output (lines):` that compares line-by-line
with whitespace normalization. Only for cross-impl tests where exact formatting is not the point.

**3c. Test formatted output as data.** Have the banner produce structured YAML; format it for display separately.
Test the data, not the display.

**Recommendation: 3a for now.** The number of formatted-output spec tests is small (banner, directory listing, script
help). The cost of keeping them in sync is low. If interactive prompts later prove untestable with exact matching,
introduce 3b at that point as a targeted extension.

### Dimension 4: The alignment wart

The current output:
```
  --help, -h      Print help on a script or directory and does not run anything
  --output, -o    Print the output at the end of the script in Yaml format
  --output-json, -j   Print the output at the end of the script in Json format
  --interactive, -i   SpecScript may prompt for user input if it needs more information
  --debug, -d     Run in debug mode. Prints stacktraces when an error occurs.
  --test, -t      Run in test mode. Only tests will be executed.
```

The descriptions on `--output, -o` and `--output-json, -j` don't line up because the width calculation ignores the
short-option suffix. Fix: change the width calculation to use the full formatted key length instead of just the property
name + 2.

Fixed output would be:
```
  --help, -h            Print help on a script or directory and does not run anything
  --output, -o          Print the output at the end of the script in Yaml format
  --output-json, -j     Print the output at the end of the script in Json format
  --interactive, -i     SpecScript may prompt for user input if it needs more information
  --debug, -d           Run in debug mode. Prints stacktraces when an error occurs.
  --test, -t            Run in test mode. Only tests will be executed.
```

This is a one-line change in Kotlin's `toDisplayString()`: compute width from the formatted key, not from the raw
property name. But it changes the spec test output in 7+ spec files. Propose as a separate cleanup.

**Recommendation:** Fix the alignment in Kotlin first (it's a one-line change), update all affected spec files, then
implement the fixed algorithm in TypeScript. This way TypeScript never has to replicate the wart.

## Proposal

1. **Fix the Kotlin alignment bug** — change `toDisplayString()` width calculation to use full key length. Update all
   affected spec files. This is a small PR.
2. **TypeScript generates from YAML** — read `specscript-command-line-options.yaml`, implement the (fixed)
   `toDisplayString()` / `infoString()` algorithm. One shared module used by both CLI and `Cli` command.
3. **Keep exact matching** — no changes to `Expected console output`. The spec shows what users see.
4. **Defer interactive prompt testing** — when we get to interactive mode, evaluate whether exact matching is viable
   or if we need a structural assertion mode.

### On the "it gets worse" concern

Interactive prompts are a genuinely harder problem because:
- Terminal interaction libraries render differently across platforms
- Cursor movement, ANSI codes, and line clearing are not capturable as plain text
- The output depends on terminal width

But this is a future problem. The current CLI work (steps 1-7) involves no interactive output — `--interactive` is
accepted but ignored. When interactive mode is implemented, the spec will need new assertion primitives regardless.
Cross that bridge then.
