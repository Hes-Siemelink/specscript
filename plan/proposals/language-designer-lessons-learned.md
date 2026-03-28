# SpecScript Language Designer: Lessons Learned

Observations from implementing SpecScript in TypeScript, for the language designer to consider.

## The Big Picture

SpecScript's core design — YAML as syntax, commands as single-key maps, variable interpolation, conditions — is sound
and ports well. The language levels decomposition proved that the feature set has a clean dependency graph. But several
behaviors are implicit, undocumented, or surprising, and they become visible only when a second implementation tries to
match the reference.

The porting exercise is the best test a language spec can get. Every ambiguity becomes a bug.

## Spec Gaps: Things That Need Documenting

### 1. The Resolve Pipeline (Critical)

The resolve pipeline is the most important and least documented part of the language. It needs its own specification
document, covering:

- **What gets resolved**: values in maps (yes), keys in maps (no), array elements (yes), strings (interpolation vs.
  whole-value replacement)
- **Resolution order**: variable substitution → eval expressions → conditions
- **When it runs**: before every command execute, unless `DelayedResolver` opts out
- **Whole-value replacement**: If the entire string is `${var}`, the result takes the type of `var` (object, array,
  number, boolean). If `${var}` is embedded in a larger string, the result is always a string.
- **Missing variable behavior**: `${obj.missing.nested}` — what happens? Kotlin returns empty string via MissingNode
  chaining. This is unspecified.

**Suggested spec change**: Add `specification/language/Resolve pipeline.spec.md` documenting all of the above with
executable examples.

### 2. Auto-List Iteration

When a command that doesn't handle lists receives an array, the runtime iterates and calls the command per-element. This
is:

- Undocumented as a language feature
- Implicit (no YAML syntax signals it)
- Inconsistently tested (few spec tests exercise commands with array input)
- Has undefined null-handling behavior (Kotlin drops nulls, TypeScript preserved them)

**Suggested spec change**: Add a section to the SpecScript overview or a dedicated `Array auto-mapping.spec.md`
explaining the behavior, listing which commands handle arrays themselves, and defining null behavior.

### 3. DelayedResolver Contract

The spec documents individual commands as "delayed resolvers" but doesn't explain what that means as a concept. A new
implementer must reverse-engineer it from Kotlin code.

**Suggested spec change**: Add a section to the resolve pipeline spec defining the DelayedResolver concept, why it
exists, and what obligations a delayed resolver command has (must call resolve explicitly on sub-data).

### 4. Error Type System

Error types are string-matched (`type: CommandError`, `type: 400`), but:

- YAML may parse `400` as an integer, not a string
- Error type names come from implementation class names (Kotlin-specific)
- The mapping from spec concepts to error type strings is undocumented

**Suggested spec change**: Define canonical error type names in the spec (not derived from implementation class names).
Add a table of error types and when they're thrown.

## Surprising Behaviors

These are behaviors that work in Kotlin but surprised the second implementer, suggesting they may also surprise users.

### 1. ForEach Loop Variable Leakage

The loop variable from `For each` leaks into the parent scope after the loop completes. This is likely a bug, but
changing it would break scripts that depend on it (e.g., accessing the last iteration's value after the loop).

**Recommendation**: Document as intentional or fix with a deprecation period.

### 2. Shell Command Default Differences

`Shell:` in YAML defaults to `show output: false`. The same command in a Markdown `shell` block defaults to `show
output: true`. Same command, different defaults based on context.

**Recommendation**: Document both defaults explicitly. Consider aligning them.

### 3. Repeat Without Termination Guard

`Repeat` with an incorrect `until` condition creates an infinite loop with no escape.

**Recommendation**: Add a configurable max iteration limit (default: 10000?), documented in the spec.

### 4. Write File Path Resolution

`Read file` resolves `file:` paths against `workingDir`. `Write file` uses the path directly (no resolution). This
asymmetry is confusing.

**Recommendation**: Make both consistent — either both resolve against workingDir or both use raw paths.

### 5. Expected Output vs. Expected Console Output

`Expected output` uses strict structural equality. `Expected console output` uses trimmed string comparison. The
difference is not documented and causes confusion when switching between them.

**Recommendation**: Document the comparison semantics explicitly for both commands.

### 6. Answers as Hidden Testing Infrastructure

The `Answers` command is essential for non-interactive testing but reads like a test utility. Its role as the mechanism
that makes `Input parameters` work in non-interactive mode is not obvious.

**Recommendation**: Document the Answers → Input parameters / Prompt interaction explicitly. Consider renaming or
restructuring.

## Feature Stories for Language Improvement

Prioritized list of improvements that would reduce surprise for both users and implementers.

### High Priority (reduce surprise)

1. **Resolve pipeline specification** — Write `Resolve pipeline.spec.md` documenting variable resolution, eval, and
   conditions. Include executable examples. This is the #1 gap.

2. **Auto-list iteration specification** — Document which commands handle lists, what happens with nulls, and how to opt
   in/out. Add to language overview.

3. **Canonical error type names** — Define error types in the spec, independent of implementation class names. Add a
   reference table.

4. **Handler type table** — Add a reference table (in the spec or `levels.yaml`) listing every command with its flags:
   `handlesLists`, `delayedResolver`, `errorHandler`. This is essential for new implementations.

### Medium Priority (improve language design)

5. **ForEach scoping** — Decide: is loop variable leakage intentional? If not, fix it. If yes, document it.

6. **Repeat termination guard** — Add a max iterations property or a built-in safety limit.

7. **File path resolution consistency** — Align Read file and Write file path resolution behavior.

8. **Shell command default alignment** — Consider making `show output` default consistent between YAML and Markdown
   contexts, or at minimum document both.

9. **YAML block scalar newline specification** — Define whether block scalars preserve or strip trailing newlines. This
   is the #1 cross-platform portability issue and caused cascading test failures in TypeScript.

### Lower Priority (nice to have)

10. **Input parameter / Prompt unification** — The relationship between `Input parameters`, `Prompt`, and `Answers` is
    confusing. Consider a unified model with explicit modes (interactive, non-interactive, test).

11. **Type coercion specification** — When is `400` a number vs. a string? Document YAML type coercion rules and where
    SpecScript applies additional coercion.

12. **`context.output` fallback documentation** — Many data manipulation commands silently read `context.output` when no
    explicit input is provided. This is undocumented and surprising.

13. **Exit semantics documentation** — `Exit` throws a special error that propagates up through control flow. Document
    which commands catch it and which propagate it.

14. **Plugin architecture** — The command handler registration pattern is already plugin-shaped. Formalize it so Level 6
    modules (SQLite, MCP) are true plugins, loadable at runtime.

## Spec Document Hygiene

### Cross-level contamination

7 out of 67 Level 0–1 spec files contain sections that use Level 3+ features (`file=` blocks, `shell cli` blocks). This
forces test runners to handle partial failures within a file. Consider:

- Moving those sections to separate files in the appropriate level, OR
- Marking cross-level sections with a `<!-- level: 3 -->` comment that test runners can use to skip

### Spec tests that assert formatted output

When spec tests assert on exact formatted output (e.g., CLI banners, Print Json formatting), any formatting quirk
becomes de-facto spec. A second implementation is forced to replicate bugs. The alignment bug in `toDisplayString()` was
caught this way — it was a bug in Kotlin that had become spec via test assertions.

**Recommendation**: Use pattern matching or partial assertions for formatted output where exact byte-matching isn't the
intent.

### YAML output format differences

Kotlin's Jackson and JavaScript's `yaml` library produce different YAML output for the same data (indentation, quoting
style, flow vs. block). Many `Expected output` tests assert on exact YAML string format. This forces implementers to
match a specific library's formatting choices.

**Recommendation**: Where possible, compare parsed structures (semantic equality) rather than formatted strings.

## What Worked Well

- **The levels system**: Clean dependency graph, 90% of files are at the right level, good bootstrap path.
- **Spec-first development**: Writing the spec before implementation caught design issues early.
- **Self-testing specification**: Once Level 2 was implemented, the specification documents became the test suite.
- **Command handler pattern**: Simple interface, easy to implement, naturally extensible.
- **The `levels.yaml` manifest**: Single source of truth for which commands and files belong to which level.

## What the TypeScript Port Improved

Several patterns discovered in the TypeScript port were backported to Kotlin:

1. **Context-based prompt dispatch** — Replaced Kotlin's global mutable `UserPrompt.default` singleton with
   context-based answers (stored in session). Cleaner, thread-safe.
2. **`Do` command list handling** — Changed from auto-iteration to native array handling (list = flat script, not
   mapped).
3. **CLI alignment bug fix** — Width calculation in `toDisplayString()` was using property name length instead of full
   formatted key length.
4. **Top-level array syntax** — Allowing `[{Print: hello}]` at the top level of YAML files.
5. **Exit propagation fix** — Control flow branches were catching `Break` (Exit error) instead of propagating it.

These demonstrate the value of a second implementation as a spec validator.
