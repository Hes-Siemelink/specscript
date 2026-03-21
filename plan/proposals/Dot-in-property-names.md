# Dot-in-property-names: variable path escaping

## Problem

SpecScript uses dot notation for navigating into nested objects: `${item.field}` resolves to the `field` property of
`item`. Under the hood, the dot path is converted to a JSON Pointer (`/item/field`) via a naive `replace('.', '/')` in
`Variables.kt:76`.

This breaks when a property name itself contains a dot. For example, given:

```yaml
${data}:
  Summary.Name: Alice
```

Writing `${data.Summary.Name}` produces JSON Pointer `/Summary/Name` — which looks for a nested `Summary` object with a
`Name` property, rather than a single property called `Summary.Name`.

JSON Pointer itself has no problem with dots in property names (it uses `/` as delimiter, and only escapes `~` and `/`
per RFC 6901). The bug is entirely in SpecScript's dot-to-slash conversion.

### Affected code

| File | Lines | What |
|------|-------|------|
| `src/main/kotlin/specscript/language/Variables.kt` | 62-68 | `splitIntoVariableAndPath` — splits at first `.` or `[` |
| `src/main/kotlin/specscript/language/Variables.kt` | 70-85 | `toJsonPointer` — blanket `replace('.', '/')` |
| `src/main/kotlin/specscript/commands/datamanipulation/Find.kt` | 14 | Also calls `toJsonPointer` for the `Find` command path |

### Where this shows up in practice

Any data source with dotted keys: REST APIs, environment variables (less common), configuration objects, or
MCP tool output with hierarchical key conventions.

## Options considered

### Option A: Backslash escaping (`\.`)

Syntax: `${data.Summary\.Name}`

A literal `\.` in the path means "this dot is part of the property name, not a separator."

**Pros:**
- Familiar from shell globbing, regex, etc.
- Minimal visual noise for the common case (no dots in keys = no change)
- Easy to explain: "escape the dot"

**Cons:**
- YAML complication: backslash is not a YAML escape character in unquoted or single-quoted strings, but it IS in
  double-quoted strings. `${data.Summary\.Name}` works fine in unquoted/single-quoted YAML, but in double-quoted YAML
  the `\` might interact with YAML's own escaping (`\\` for literal backslash). This is manageable since SpecScript
  variables are typically in unquoted strings, but it's a subtle gotcha.
- Parser becomes stateful — can't just `split('.')` anymore, need to walk character by character or use a regex that
  respects escapes.

**Implementation sketch:**

```kotlin
fun splitPath(path: String): List<String> {
    val segments = mutableListOf<String>()
    val current = StringBuilder()
    var i = 0
    while (i < path.length) {
        when {
            path[i] == '\\' && i + 1 < path.length && path[i + 1] == '.' -> {
                current.append('.')
                i += 2
            }
            path[i] == '.' -> {
                segments.add(current.toString())
                current.clear()
                i++
            }
            path[i] == '[' -> {
                // Array index handling
                if (current.isNotEmpty()) {
                    segments.add(current.toString())
                    current.clear()
                }
                val end = path.indexOf(']', i)
                segments.add(path.substring(i + 1, end))  // numeric index
                i = end + 1
            }
            else -> {
                current.append(path[i])
                i++
            }
        }
    }
    if (current.isNotEmpty()) segments.add(current.toString())
    return segments
}
```

Then `toJsonPointer` builds from segments, applying JSON Pointer escaping (`~` → `~0`, `/` → `~1`) per RFC 6901.

### Option B: Bracket notation (`["key"]`)

Syntax: `${data["Summary.Name"]}`

Borrow JavaScript's bracket notation for property access when dot notation is ambiguous.

**Pros:**
- Very familiar to JavaScript/Python developers
- Clearly delineates the key — no ambiguity, no escaping needed within the brackets
- Could also solve future issues like spaces in property names: `${data["first name"]}`

**Cons:**
- Heavier syntax for a simple case
- Parsing is more complex: need to handle quoted strings inside brackets, both `["key"]` and `['key']`
- Mixes two notations — users need to learn both
- The existing `[0]` array index syntax already uses brackets without quotes; adding `["key"]` means the parser
  must distinguish `[0]` (index) from `["key"]` (property)
- More invasive change to the regex-based parser

**Implementation sketch:**

Extend the path parser to handle:
- `.name` — unquoted property (no dots allowed in name)
- `["name.with.dots"]` — quoted property
- `[0]` — array index

### Option C: Alternate delimiter (e.g., `->` or `/`)

Syntax: `${data/Summary.Name}` or `${data->Summary.Name}`

Use a different character as the path separator.

**Pros:**
- Sidesteps the escaping problem entirely
- `/` would align with JSON Pointer natively

**Cons:**
- Breaking change for all existing scripts
- `/` is already used in YAML and file paths — visual confusion
- `->` is unusual in YAML contexts
- Loses the "easy on the eyes" quality of dot notation

**Rejected.** Breaking change, worse readability.

### Option D: Hybrid — backslash escaping + bracket notation

Support both `\.` and `["key"]` as complementary mechanisms.

**Pros:**
- `\.` for the simple/common case (one dot in a key)
- `["key"]` for ugly keys (multiple dots, spaces, brackets)
- Maximum flexibility

**Cons:**
- Two mechanisms to implement, document, and maintain
- Overkill for the current problem

## Recommendation: Option A (backslash escaping), with Option B deferred

**Start with `\.` escaping.** It is the minimal change that solves the problem, is easy to explain, and doesn't disturb
existing syntax. The YAML interaction is manageable because SpecScript variables live in unquoted strings the vast
majority of the time.

Bracket notation (Option B) can be added later if real-world use reveals keys that are painful even with escaping
(spaces, brackets, etc.). The two mechanisms are complementary and non-conflicting.

## Implementation plan

### Phase 1: Core parser (Variables.kt)

1. **Replace `toJsonPointer`** with a proper segment-based parser that:
   - Walks the path character by character
   - Treats `\.` as a literal dot within a segment
   - Treats unescaped `.` as a segment separator
   - Treats `[N]` as an array index segment
   - Builds a `JsonPointer` from segments using Jackson's API (which handles RFC 6901 escaping of `~` and `/` in
     property names)

2. **Update `splitIntoVariableAndPath`** — no change needed; it splits at the first unescaped `.` or `[`, which
   already works because `\.` would appear after the variable name, not in it. Actually — wait. If the path starts with
   `\.`, `splitIntoVariableAndPath` would still split at the `\` incorrectly because its regex `(.*?)([\[.].*$)` matches
   at the `.` in `\.`. This needs updating too: the regex must skip `\.` sequences.

   Updated regex approach: instead of a simple regex, walk the characters or use a negative lookbehind:
   `(.*?)(?<!\\)([\[.].*$)`. But lookbehinds and escapes interact poorly. Better: parse character-by-character in
   `splitIntoVariableAndPath` as well, splitting at the first unescaped `.` or `[`.

### Phase 2: Find command

3. **Verify `Find.kt`** — it passes a user-provided `path` string to `toJsonPointer`. With the new parser, escaped
   dots will work automatically. No code change needed beyond what Phase 1 provides.

### Phase 3: Unit tests (VariablesTest.kt)

4. **Add test cases for `splitIntoVariableAndPath`:**
   - `"data.Summary\\.Name"` → name=`"data"`, path=`".Summary\\.Name"`
   - `"data.ok"` → unchanged behavior

5. **Add test cases for `toJsonPointer`:**
   - `".Summary\\.Name"` → `/Summary.Name` (single segment with literal dot)
   - `".a\\.b.c"` → `/a.b/c` (mixed escaped and unescaped)
   - `".a\\.b[0].c"` → `/a.b/0/c`

6. **Existing tests must pass unchanged** — no regression.

### Phase 4: Specification

7. **Update `specification/language/Variables.spec.md`** — add a subsection to "Path reference":

   ```markdown
   ### Property names with dots

   If a property name contains a dot, escape it with a backslash: `\.`

   ⟨executable example showing the feature⟩
   ```

8. **Run `./gradlew specificationTest`** to validate.

### Phase 5: Edge cases to handle

- `\\` (literal backslash before a dot): `\\\\.` should produce a literal backslash followed by a path separator.
  Decide: is `\\` an escape for backslash itself? Recommendation: yes, for consistency. `\\` → literal `\`, `\.` →
  literal `.`. This keeps the escaping system self-consistent.
- Backslash before any other character: treat as literal backslash (not an error). Only `\.` and `\\` are special.
- Empty segments: `${data..field}` — currently produces an empty segment. Keep existing behavior (or error). Not
  affected by this change.

## Risks

- **YAML double-quoted strings**: In `"${data.Summary\.Name}"`, YAML itself doesn't process `\.` specially (backslash
  is only special for `\\`, `\"`, `\n`, etc. in double-quoted YAML). So `\.` passes through to SpecScript as-is. No
  conflict.
- **Existing scripts**: No existing script uses `\.` in variable paths today (it would be a syntax error or nonsensical),
  so this is purely additive.
- **Find command path strings**: The `Find` command takes the path as a YAML string value. Same YAML escaping
  considerations apply; same conclusion — no conflict.

## Estimated scope

- ~30 lines of Kotlin changed in `Variables.kt`
- ~20 lines of new unit tests
- ~15 lines of new specification content
- No new files, no new dependencies
