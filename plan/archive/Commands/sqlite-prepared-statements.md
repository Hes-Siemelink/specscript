# Proposal: SQLite Prepared Statement Parameters

## Problem

Every SpecScript script that takes user input and passes it to SQLite must manually escape single quotes to prevent SQL
injection:

```yaml
Replace:
  text: "'"
  in: ${input}
  with: "''"
As: ${input}

SQLite:
  update:
    - INSERT INTO goal (title) VALUES ('${input.title}')
```

This is fragile (only handles single quotes), verbose (4 lines of boilerplate per script), and error-prone (easy to
forget). All 20+ sample scripts that use SQLite with user input have this same pattern.

## Proposed Solution

Make the SQLite command a `DelayedResolver` so it receives raw YAML with `${...}` references still as literal text.
Before executing SQL, the command transforms quoted variable references into prepared statement parameters:

**`'${var}'` in SQL → `?` placeholder, with the resolved variable value passed as a parameter.**

Unquoted `${var}` references (used for table names, column names, etc.) are resolved inline as today, with identifier
validation to prevent structural injection.

### Transformation example

Script author writes (unchanged from today):

```yaml
SQLite:
  update:
    - INSERT INTO goal (title, priority) VALUES ('${input.title}', '${input.priority}')
  query: SELECT * FROM goal WHERE id = '${input.id}'
```

The command internally transforms this to:

```
update: INSERT INTO goal (title, priority) VALUES (?, ?)    params: [input.title, input.priority]
query:  SELECT * FROM goal WHERE id = ?                     params: [input.id]
```

### Detection rule

A regex finds `'${...}'` patterns (variable reference wrapped in single quotes). Each match:
1. The entire `'${...}'` is replaced with `?`
2. The variable reference is resolved from context
3. The resolved value is added to the parameter list

Any remaining (unquoted) `${...}` references are resolved inline via normal variable resolution.

### Affected fields

All SQL-carrying fields get the treatment:
- `SQLite` command: `update:` (list of strings) and `query:` (string)
- `Store` command: `query.where:` (string — already contains `json_extract()` expansions, but variable values within
  it should be parameterized)

### What about Store inserts?

`Store.insert:` already receives structured data (YAML objects), not SQL strings. The TypeScript implementation already
uses prepared statements for these (`INSERT INTO table (json) VALUES (?)`). No change needed.

## Scope

**In scope:**
- Make SQLite a DelayedResolver
- Transform quoted variable references to prepared statement parameters in update/query
- Apply same treatment to Store's where clause
- Remove the Replace/As quote-escaping boilerplate from all sample scripts (since parameterization makes it unnecessary
  and leaving it in would double-escape: the `''` would be stored literally via the prepared statement)
- Write specification (this is currently spec-less — only test files exist)
- Implement in both Kotlin and TypeScript

**Out of scope:**
- Identifier validation for unquoted `${var}` in structural positions (table names, column names) — good idea but
  separate concern
- Full SQL parsing — the regex approach handles all existing patterns
- Changes to the Store insert path (already parameterized)

## Impact

- **Existing scripts:** No breaking changes to the SQLite syntax itself. The `'${var}'` pattern is already what every
  sample uses. However, scripts that include the Replace/As quote-escaping boilerplate must be updated — the doubled
  quotes (`''`) would now be stored literally via the prepared statement instead of being unescaped by SQL parsing.
  All affected samples will be updated as part of this work.
- **New scripts:** No longer need manual quote escaping. Just write `'${input.name}'` and it works safely.
- **Specification:** SQLite and Store commands currently have no spec document (only test files). This is a good
  opportunity to write the spec.
