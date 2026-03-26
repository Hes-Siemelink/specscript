import {
  type JsonValue, type JsonObject,
  isObject, isArray, isString,
  SpecScriptError, toDisplayString,
} from './types.js'

const VARIABLE_REGEX = /\$\{([^}]+)}/g
const FULL_VARIABLE_REGEX = /^\$\{([^}]+)}$/

/**
 * Resolve all ${...} variable references in a JSON tree.
 * - Full replacement: "${varName}" → variable value (preserves type)
 * - Interpolation: "Hello ${name}!" → string substitution
 */
export function resolveVariables(node: JsonValue, variables: Map<string, JsonValue>): JsonValue {
  if (isString(node)) {
    return resolveStringVariables(node, variables)
  }
  if (isArray(node)) {
    return node.map(item => resolveVariables(item, variables))
  }
  if (isObject(node)) {
    const result: JsonObject = {}
    for (const [key, value] of Object.entries(node)) {
      // Keys are NOT resolved — only values. Matches Kotlin's JsonProcessor behavior.
      result[key] = resolveVariables(value, variables)
    }
    return result
  }
  return node
}

/**
 * Resolve a string that may contain variable references.
 * If the entire string is a single ${var}, return the variable's typed value.
 * Otherwise, interpolate as text.
 */
function resolveStringVariables(text: string, variables: Map<string, JsonValue>): JsonValue {
  // Check for full replacement: entire string is "${varName}"
  const fullMatch = FULL_VARIABLE_REGEX.exec(text)
  if (fullMatch) {
    const varExpr = fullMatch[1]
    return lookupVariable(varExpr, variables)
  }

  // Inline interpolation: replace each ${var} with its string representation
  return resolveStringValue(text, variables)
}

/**
 * Resolve variable references in a string, always returning a string.
 */
function resolveStringValue(text: string, variables: Map<string, JsonValue>): string {
  return text.replace(VARIABLE_REGEX, (_match, varExpr: string) => {
    const value = lookupVariable(varExpr, variables)
    return toDisplayString(value)
  })
}

/**
 * Look up a variable by expression, supporting path navigation.
 * Expression can be: "varName", "varName.field", "varName[0]", "varName[\"key\"]"
 */
function lookupVariable(expression: string, variables: Map<string, JsonValue>): JsonValue {
  const { name, path } = splitVariableAndPath(expression)
  const value = variables.get(name)
  if (value === undefined) {
    throw new SpecScriptError(`Unknown variable: ${name}`)
  }
  if (path === undefined) return value
  return navigatePath(value, path, expression)
}

/**
 * Split a variable expression into the variable name and optional path.
 * "user.name" → { name: "user", path: ".name" }
 * "items[0]" → { name: "items", path: "[0]" }
 * "simple" → { name: "simple", path: undefined }
 */
function splitVariableAndPath(expression: string): { name: string; path?: string } {
  const match = expression.match(/^(.*?)([\[.].*$)/)
  if (match) {
    return { name: match[1], path: match[2] }
  }
  return { name: expression }
}

/**
 * Navigate a path expression on a JSON value.
 * Supports dot notation (.field) and bracket notation ([0], ["key"]).
 */
function navigatePath(value: JsonValue, path: string, fullExpression: string): JsonValue {
  let current: JsonValue = value
  // Tokenize path into segments: .field, [0], ["key"], ['key']
  const segmentRegex = /\.([^.[]+)|\[(\d+)]|\["([^"]+)"]|\['([^']+)']/g
  let match: RegExpExecArray | null

  while ((match = segmentRegex.exec(path)) !== null) {
    const key = match[1] ?? match[3] ?? match[4] // dot or bracket-string access
    const index = match[2] // numeric index

    if (index !== undefined) {
      if (!isArray(current)) {
        throw new SpecScriptError(`Cannot index non-array with [${index}] in \${${fullExpression}}`)
      }
      const i = parseInt(index, 10)
      if (i < 0 || i >= current.length) {
        throw new SpecScriptError(`Array index ${i} out of bounds in \${${fullExpression}}`)
      }
      current = current[i]
    } else if (key !== undefined) {
      if (!isObject(current)) {
        throw new SpecScriptError(`Cannot access property '${key}' on non-object in \${${fullExpression}}`)
      }
      if (!(key in current)) {
        // Missing property — return empty string (like Kotlin's MissingNode)
        return ''
      }
      current = current[key]
    }
  }

  return current
}
