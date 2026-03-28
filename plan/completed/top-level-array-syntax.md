# Top-level array syntax in SpecScript YAML files

## Problem

Currently, to write multiple commands in a `.spec.yaml` file, users must either:

1. Use `---` document separators between commands
2. Wrap commands in a `Do:` block with list syntax

Both work, but YAML list syntax is a natural and familiar way to express a sequence of commands. Users may intuitively
write:

```yaml
- Print: Hello
- Print: world
```

This is currently silently ignored because the parser only handles object nodes at the top level.

## Proposed solution

Allow YAML arrays at the top level of a SpecScript document. Each array element is treated as a command object, identical
to how the `Do` command processes its list content.

### Equivalences

These three forms would all produce the same result:

```yaml
# Form 1: Array syntax (NEW)
- Print: Hello
- Print: world
```

```yaml
# Form 2: Document separator (existing)
Print: Hello
---
Print: world
```

```yaml
# Form 3: Do wrapper (existing)
Do:
  - Print: Hello
  - Print: world
```

### Implementation

The change is in `Script.kt`, function `toCommandList(scriptNode: JsonNode)` (line 111-113). When the node is an
`ArrayNode`, iterate its elements and extract commands from each element (which should be an object node).

### Scope

- Top-level array in a YAML document (each `---`-separated document can independently be a map or array)
- Each array element must be a single-key object (the command name and its data)
- Mixed documents (some maps, some arrays) work naturally since each document is processed independently
