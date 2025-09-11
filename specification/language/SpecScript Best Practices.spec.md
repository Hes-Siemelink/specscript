# SpecScript Best Practices

This document outlines best practices for writing effective SpecScript specifications.

## Keep It Flat

**Principle**: Avoid nesting commands within other commands that have both scalar and object content.

SpecScript follows a "flat" structure where commands are typically at the same indentation level. This makes specifications easier to read, understand, and maintain.

### ❌ Avoid: Nested command structure

```yaml
Test case: # Create greeting and verify result
  POST:
    url: http://localhost:2525/greeting
    body:
      name: Alice
      language: English
  
  Expected output: Hi Alice!
```


### ✅ Preferred: Flat command structure

```yaml specscript
Test case: Create greeting and verify result

POST:
  url: http://localhost:2525/greeting
  body:
    name: Alice
    language: English

As: ${greeting_result}

Expected output: Hi Alice!
```

**Benefits**:
- Commands at consistent indentation level
- Easier to read and scan

### Why Keep It Flat?

1. **Simplicity**: Flat structures are easier to understand at a glance
2. **Consistency**: All commands follow the same pattern
3. **Maintainability**: Less nesting means fewer opportunities for structural errors
4. **Readability**: Business stakeholders can more easily follow the flow

## Common Pitfalls

### YAML Duplicate Key Constraints

**Problem**: Repeating command names is invalid YAML
```yaml
# ❌ This won't work - duplicate keys
Assert that:
  item: value1
  equals: expected1
Assert that:  # Invalid YAML!
  item: value2  
  equals: expected2
```

**Solutions**:
```yaml
# ✅ Use list syntax
Assert that:
- item: value1
  equals: expected1
- item: value2
  equals: expected2

# ✅ OR use document separators
Assert that:
  item: value1
  equals: expected1
---
Assert that:
  item: value2
  equals: expected2
```

### Understanding Content Type Support

When command specs show "List | implicit", it means the command can handle list syntax automatically:

```yaml
# Single execution
Print: Hello

---
# Multiple executions (list syntax)
Print:
- Hello
- World
```

### Temp File Referencing

**For markdown temp files**: Use `resource:` parameter
```yaml
# After creating: ```yaml file=config.json
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

---

*More best practices will be added to this document as they are identified.*