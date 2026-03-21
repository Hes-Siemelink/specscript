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

**Rejected.** Breaking change for all existing scripts, worse readability.

### Option D: Hybrid — backslash escaping + bracket notation

**Rejected.** Two mechanisms to implement, document, and maintain. Overkill.

## Decision: Option B (bracket notation)

Option A (backslash escaping) was prototyped and rejected — too hacky and not scalable. Backslash interacts with YAML's
own escaping in double-quoted strings, requires escape-the-escape (`\\`) for literal backslashes, and only solves dots.

**Bracket notation wins because:**
- Familiar from JavaScript/Python
- Handles dots, spaces, and any future special characters with one mechanism
- No escaping edge cases — the key is just a quoted string
- `splitIntoVariableAndPath` didn't need changes (existing regex already splits at `[`)
- Parser is simpler (no escape state tracking)

## Implementation

Replaced `toJsonPointer` with a segment-based `parsePath()` that handles:
- `.name` — unquoted dot-separated property
- `[0]` — numeric array index
- `["key"]` — quoted property name (any characters allowed)

Segments are assembled into a JSON Pointer with RFC 6901 escaping (`~` → `~0`, `/` → `~1`).
