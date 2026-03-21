---
name: specscript-input-handling
description: Input handling architecture in SpecScript. InputSchema, InputParameters, DelayedResolver, variable resolution, MCP tool schema derivation. Use when working on input/parameter handling, the DelayedResolver interface, or MCP tool definitions.
compatibility: Requires the specscript project structure.
metadata:
  author: specscript
  version: "1.0"
---

## Overview

SpecScript has two commands for defining script input: `Input parameters` (legacy) and `Input schema` (recommended).
Both populate the `${input}` variable identically.

## Input schema (preferred for new scripts)

- Uses standard JSON Schema syntax (`type: object`, `properties`, `required`)
- Implemented in `InputSchema.kt` — implements `ObjectHandler` + `DelayedResolver`
- Converts JSON Schema `properties` into `ParameterData` objects, then delegates to
  `InputParameters.populateInputVariables()` for resolution
- Only a narrow JSON Schema subset is supported: `description`, `default`, `enum`, `type` (informational),
  `condition` (SpecScript extension) per property; `type`, `properties`, `required` at top level
- `ParameterData.schema.yaml` is shared between both commands via `$ref`

## Variable resolution priority

`populateInputVariables()` resolves each property in order:

1. Existing value (already set in context)
2. Default value from schema
3. Recorded test answer
4. Interactive prompt
5. `MissingInputException`

## DelayedResolver interface

Commands implementing `DelayedResolver` receive raw YAML data without variable expansion. The command handles
`${variable}` resolution itself. Both `InputParameters` and `InputSchema` use this because property definitions may
contain variable references (e.g., `condition: { item: ${input.switch} }`).

## Script metadata extraction

`Script.getScriptInfo()` scans command lists for both `Input parameters` and `Input schema` to extract metadata for CLI
`--help`, MCP tool schema derivation, and test scaffolding.

**Important**: `ScriptInfoData` does not preserve the `required` array — `deriveInputSchema()` in `McpServer.kt`
reads raw command data from `Script.commands` instead.

## MCP tool schema derivation

When an MCP tool references a script file and has no explicit `inputSchema`, `McpServer.deriveInputSchema()` loads the
script and extracts its `Input schema` (falling back to `Input parameters`). This eliminates schema duplication between
script input definitions and MCP tool definitions.

## Two independent validation systems

1. **JSON Schema validation** (networknt library) — validates command YAML structure against `schema/*.schema.yaml`
2. **SpecScript type system** (`TypeRegistry`/`TypeSpecification`) — runtime type checking for variables

These systems do not interact.
