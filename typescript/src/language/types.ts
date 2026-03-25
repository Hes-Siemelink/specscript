// SpecScript value types — native JSON types with type guards

export type JsonValue = string | number | boolean | null | JsonValue[] | JsonObject
export type JsonObject = { [key: string]: JsonValue }

export function isObject(v: JsonValue): v is JsonObject {
  return v !== null && typeof v === 'object' && !Array.isArray(v)
}

export function isArray(v: JsonValue): v is JsonValue[] {
  return Array.isArray(v)
}

export function isString(v: JsonValue): v is string {
  return typeof v === 'string'
}

export function isNumber(v: JsonValue): v is number {
  return typeof v === 'number'
}

export function isBoolean(v: JsonValue): v is boolean {
  return typeof v === 'boolean'
}

export function isNull(v: JsonValue): v is null {
  return v === null
}

/** A scalar value (string, number, boolean, null) — not an object or array */
export function isScalar(v: JsonValue): v is string | number | boolean | null {
  return !isObject(v) && !isArray(v)
}

/** A parsed command: name + data payload */
export interface Command {
  name: string
  data: JsonValue
}

/**
 * Convert a JsonValue to its display string (like Kotlin's toDisplayYaml).
 * For complex types (arrays/objects), set the formatter via setDisplayFormatter().
 */
export function toDisplayString(v: JsonValue): string {
  if (v === null) return 'null'
  if (typeof v === 'string') return v
  if (typeof v === 'number' || typeof v === 'boolean') return String(v)
  if (_displayFormatter) return _displayFormatter(v)
  return JSON.stringify(v)
}

/** Formatter function for complex values. Set by the YAML module at init time. */
let _displayFormatter: ((v: JsonValue) => string) | undefined

/** Register a display formatter for complex JSON values. */
export function setDisplayFormatter(fn: (v: JsonValue) => string): void {
  _displayFormatter = fn
}

/** Deep clone a JsonValue */
export function deepCopy(v: JsonValue): JsonValue {
  if (v === null || typeof v !== 'object') return v
  return JSON.parse(JSON.stringify(v))
}

/** Get a required parameter from an object, throw if missing */
export function getParameter(obj: JsonObject, name: string): JsonValue {
  if (!(name in obj)) {
    throw new CommandFormatError(`Missing required parameter: ${name}`)
  }
  return obj[name]
}

/** Get a required string parameter from an object */
export function getTextParameter(obj: JsonObject, name: string): string {
  const value = getParameter(obj, name)
  if (typeof value !== 'string') {
    throw new CommandFormatError(`Parameter '${name}' must be a string, got ${typeof value}`)
  }
  return value
}

// --- Error hierarchy ---

/** Base error for all SpecScript errors */
export class SpecScriptError extends Error {
  command?: string
  constructor(message: string, options?: { cause?: Error }) {
    super(message, options)
    this.name = 'SpecScriptError'
  }
}

/** Invalid command structure or arguments */
export class CommandFormatError extends SpecScriptError {
  constructor(message: string) {
    super(message)
    this.name = 'CommandFormatError'
  }
}

/** User-facing, handleable error (thrown by Error command, caught by On error / Expected error) */
export class SpecScriptCommandError extends SpecScriptError {
  type: string
  data?: JsonValue

  constructor(message: string, type: string = 'error', data?: JsonValue, cause?: Error) {
    super(message, { cause })
    this.name = 'SpecScriptCommandError'
    this.type = type
    this.data = data
  }
}

/** Internal error — wraps unexpected exceptions */
export class SpecScriptInternalError extends SpecScriptError {
  constructor(message: string, cause?: Error) {
    super(message, { cause })
    this.name = 'SpecScriptInternalError'
  }
}

/** Missing required input parameter */
export class MissingInputError extends SpecScriptError {
  paramName: string
  constructor(message: string, paramName: string) {
    super(message)
    this.name = 'MissingInputError'
    this.paramName = paramName
  }
}

/** Flow control: thrown by Exit to short-circuit script execution */
export class Break extends Error {
  output: JsonValue
  constructor(output: JsonValue) {
    super('Break')
    this.name = 'Break'
    this.output = output
  }
}

/** Thrown when Expected error didn't find an error */
export class MissingExpectedError extends SpecScriptCommandError {
  constructor(message: string) {
    super(message, 'missing-expected-error')
    this.name = 'MissingExpectedError'
  }
}
