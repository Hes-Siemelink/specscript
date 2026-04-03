---
# specscript-m2bu
title: 'Bug: Eval syntax (/Command) leaks output into parent scope'
status: todo
type: bug
priority: high
created_at: 2026-04-03T08:23:46Z
updated_at: 2026-04-03T08:23:46Z
---

When using eval syntax (e.g. /Size inside a Print data block), the evaluated command sets ${output} in the parent scope. This is a scope leak — eval should be side-effect-free. Example: after 'Print: { Items: { /Size: ${list} } }', ${output} is set to the size value, which silently corrupts any subsequent use of ${output}. Likely requires changes to the command execution loop to isolate eval execution from the parent variable context.
