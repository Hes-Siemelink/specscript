# SpecScript Best Practices

This document outlines best practices for writing effective SpecScript specifications.

## Common Pitfalls

### Use `---` separators liberally

SpecScript commands are YAML dictionary keys. Repeating a key is invalid YAML — a silent data-loss bug in most parsers.
The `---` document separator avoids this and doubles as a visual section divider.

```yaml
# ❌ Invalid YAML — duplicate key
Print: Hello
Print: Hello again!
```

```yaml
# ✅ Separate documents
Print: Hello
---
Print: Hello again!
```

This applies to **any** key that appears more than once at the same level, including `As`, `Add to`, `If`, `For each`,
and `Size`. A common mistake is two commands that each use `As` in the same document:

```yaml
# ❌ Invalid — duplicate As key
GET: /api/users
As: ${users}

For each:
  ${u} in: ${users}
  Output: ${u.name}
As: ${names}
```

```yaml
# ✅ Split into separate documents
GET: /api/users
As: ${users}
---
For each:
  ${u} in: ${users}
  Output: ${u.name}
As: ${names}
```

As a rule of thumb: when in doubt, add a `---`. It never hurts and prevents subtle bugs.

### Prefer `---` over list syntax

Top-level list syntax (`- Print: Hello`) avoids duplicate keys but adds visual noise. The `---` separator is easier
to scan and acts as a natural section break:

```yaml
# Noisy
- Print: Hello
- Print: Hello again!
- Print: Goodbye
```

```yaml
# Cleaner
Print: Hello
---
Print: Hello again!
---
Print: Goodbye
```

### Use `---` instead of comments for section breaks

Comments like `# --- Section ---` are redundant when `---` separators are already present. Let the separators do
double duty — they enforce valid YAML and visually divide the script:

```yaml
# ❌ Redundant comment separators
# --- Set up connection ---
Http request defaults:
  url: https://api.example.com

# --- Fetch data ---
GET: /items
As: ${items}
```

```yaml
# ✅ Let --- do the work
Http request defaults:
  url: https://api.example.com

---
GET: /items
As: ${items}
```

### Temp File Referencing

**For markdown temp files**: Use `resource:` parameter

```yaml
# After creating: ```yaml temp-file=config.json
Read file:
  resource: config.json  # ✅ Correct for temp files
---
# Don't use ${SCRIPT_TEMP_DIR} for markdown temp files
Read file: ${SCRIPT_TEMP_DIR}/config.json  # Also correct, but unnecessary
```

**For external files**: Use direct paths

```yaml
Read file: path/to/external/file.yaml  # ✅ For real files
```

