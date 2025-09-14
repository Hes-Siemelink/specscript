# SpecScript Markdown tests

This document specifies the nitty-gritty around writing SpecScript files in Markdown.

## Hidden before blocks

Hidden code blocks that become before actual code may contain a script that contains `---` dividers.

<!-- yaml specscript
${one}: one
---
${two}: two
-->

```yaml specscript
Test case: Hidden before block with dividers

Assert that:
  - item: ${one}
    equals: one
  - item: ${two}
    equals: two
```