import { parseAllDocuments, parseDocument, isMap, isSeq, isScalar, isPair, Pair, type Document, type Node } from 'yaml'
import type { JsonValue, JsonObject } from '../language/types.js'
import type { Command } from '../language/types.js'

/**
 * Parse a YAML string as a multi-document SpecScript file.
 * Returns a flat list of commands, preserving duplicate keys.
 *
 * Unlike standard YAML-to-JSON conversion, this preserves duplicate keys
 * in mappings as separate commands (SpecScript's command model).
 *
 * When `stripBlockScalarNewlines` is true, trailing newlines are stripped from
 * YAML block scalar (| and >) string values. This matches Jackson's behavior
 * when parsing YAML from a string that doesn't end with a newline (e.g.,
 * content from Markdown getContent()). The JS yaml library always appends a
 * trailing newline to block scalar values regardless of source termination.
 */
export function parseYamlCommands(content: string, stripBlockScalarNewlines: boolean = false): Command[] {
  const docs = parseAllDocuments(content, { uniqueKeys: false })
  const commands: Command[] = []

  for (const doc of docs) {
    if (doc.errors.length > 0) {
      throw new Error(`YAML parse error: ${doc.errors[0].message}`)
    }
    const node = doc.contents
    if (node === null || node === undefined) continue

    if (isMap(node)) {
      // Each key-value pair in a mapping is a separate command
      for (const pair of node.items) {
        if (isPair(pair)) {
          const name = scalarToString(pair.key)
          const data = nodeToJson(pair.value, stripBlockScalarNewlines)
          commands.push({ name, data })
        }
      }
    } else if (isSeq(node)) {
      // Array: each element is recursively converted
      for (const item of node.items) {
        if (isMap(item)) {
          for (const pair of item.items) {
            if (isPair(pair)) {
              commands.push({ name: scalarToString(pair.key), data: nodeToJson(pair.value, stripBlockScalarNewlines) })
            }
          }
        }
        // Skip non-map array items at top level
      }
    }
  }

  return commands
}

/**
 * Parse a YAML string as a multi-document file, returning JsonValue per document.
 * Standard conversion — duplicate keys are last-wins.
 * Used for non-command YAML (data values, config, etc.)
 */
export function parseYamlFile(content: string): JsonValue[] {
  const docs = parseAllDocuments(content, { uniqueKeys: false })
  const results: JsonValue[] = []
  for (const doc of docs) {
    if (doc.errors.length > 0) {
      throw new Error(`YAML parse error: ${doc.errors[0].message}`)
    }
    const value = doc.toJSON()
    if (value !== undefined && value !== null) {
      results.push(value as JsonValue)
    }
  }
  return results
}

/**
 * Parse a single YAML document.
 */
export function parseYaml(content: string): JsonValue {
  const docs = parseYamlFile(content)
  if (docs.length === 0) return null
  return docs[0]
}

/**
 * Convert a value to its YAML display string.
 * For strings, returns the raw string (no quotes).
 * For other types, uses YAML serialization.
 */
export function toDisplayYaml(value: JsonValue): string {
  if (value === null) return 'null'
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)

  // Use YAML stringify for objects and arrays
  const { stringify } = require('yaml') as typeof import('yaml')
  const result = stringify(value, {
    lineWidth: 0,        // don't wrap lines
    minContentWidth: 0,
  })
  // Remove trailing newline that yaml library adds
  return result.replace(/\n$/, '')
}

/**
 * Try to parse a string as YAML, falling back to a plain string.
 */
export function parseYamlIfPossible(source: string | null | undefined): JsonValue {
  if (source === null || source === undefined) return null
  try {
    return parseYaml(source)
  } catch {
    return source
  }
}

// --- Internal helpers ---

function scalarToString(node: unknown): string {
  if (isScalar(node)) return String(node.value)
  return String(node)
}

/** Convert a yaml AST node to a plain JsonValue, preserving duplicate-key maps as last-wins. */
function nodeToJson(node: unknown, stripBlockScalarNewlines: boolean = false): JsonValue {
  if (node === null || node === undefined) return null
  if (isScalar(node)) {
    const v = node.value
    if (v === null || v === undefined) return null
    if (typeof v === 'string') {
      // Strip trailing newline from block scalar values (| and >) when flag is set.
      // The JS yaml library always appends \n to block scalars, but Jackson only does
      // so when the source string ends with \n. Since getContent() never ends with \n,
      // we strip to match Jackson's behavior for Markdown-sourced content.
      if (stripBlockScalarNewlines && (node.type === 'BLOCK_LITERAL' || node.type === 'BLOCK_FOLDED')) {
        return v.replace(/\n$/, '')
      }
      return v
    }
    if (typeof v === 'number' || typeof v === 'boolean') return v
    return String(v)
  }
  if (isMap(node)) {
    const obj: JsonObject = {}
    for (const pair of node.items) {
      if (isPair(pair)) {
        obj[scalarToString(pair.key)] = nodeToJson(pair.value, stripBlockScalarNewlines)
      }
    }
    return obj
  }
  if (isSeq(node)) {
    return node.items.map((item: unknown) => nodeToJson(item, stripBlockScalarNewlines))
  }
  // Fallback
  if (typeof node === 'object' && node !== null && 'toJSON' in node) {
    return (node as { toJSON(): JsonValue }).toJSON()
  }
  return null
}
