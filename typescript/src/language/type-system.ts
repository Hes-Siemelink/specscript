/**
 * SpecScript type system — validates data against type definitions.
 *
 * Types can be:
 * - Base types: string, boolean, object, array
 * - Named types: loaded from types.yaml in the script directory
 * - Inline types: defined directly in the Check type command
 *
 * Mirrors the Kotlin implementation in specscript.language.types.
 */

import type { JsonValue, JsonObject } from './types.js'
import { isObject, isArray, isString } from './types.js'

// --- Type specification (what you write in YAML) ---

export interface TypeSpecification {
  /** Reference to a named type (e.g., "string", "Person") */
  name?: string
  /** Base type: string, boolean, object, array */
  base?: string
  /** Properties for object types */
  properties?: Record<string, PropertySpecification>
  /** Item type for array types */
  listOf?: TypeSpecification
}

export interface PropertySpecification {
  type?: TypeSpecification
  optional?: boolean
}

// --- Type (resolved, named wrapper) ---

export interface Type {
  name: string
  definition: TypeSpecification
}

// --- Type Registry ---

export class TypeRegistry {
  private types = new Map<string, Type>()

  constructor() {
    this.registerType({ name: 'string', definition: { base: 'string' } })
    this.registerType({ name: 'boolean', definition: { base: 'boolean' } })
    this.registerType({ name: 'object', definition: { base: 'object' } })
    this.registerType({ name: 'array', definition: { base: 'array' } })
  }

  registerType(type: Type): void {
    this.types.set(type.name, type)
  }

  getType(name: string): Type | undefined {
    return this.types.get(name)
  }
}

// --- Parsing: YAML data -> TypeSpecification ---

/**
 * Parse a YAML value into a TypeSpecification.
 * Handles both string (type name) and object (inline definition) forms.
 */
export function parseTypeSpecification(data: JsonValue): TypeSpecification {
  if (isString(data)) {
    return { name: data }
  }
  if (isObject(data)) {
    const spec: TypeSpecification = {}
    if (typeof data['base'] === 'string') spec.base = data['base']
    if (typeof data['name'] === 'string') spec.name = data['name']
    if (isObject(data['properties'])) {
      spec.properties = parseProperties(data['properties'])
    }
    const listOfValue = data['list of']
    if (listOfValue !== undefined) {
      spec.listOf = parseTypeSpecification(listOfValue)
    }
    // If properties are defined but no base, infer object
    if (spec.properties && !spec.base && !spec.name) {
      spec.base = 'object'
    }
    return spec
  }
  throw new Error(`Invalid type specification: ${JSON.stringify(data)}`)
}

function parseProperties(data: JsonObject): Record<string, PropertySpecification> {
  const result: Record<string, PropertySpecification> = {}
  for (const [key, value] of Object.entries(data)) {
    result[key] = parsePropertySpecification(value)
  }
  return result
}

function parsePropertySpecification(data: JsonValue): PropertySpecification {
  if (isString(data)) {
    // Short form: just a type name
    return { type: { name: data } }
  }
  if (isObject(data)) {
    const spec: PropertySpecification = {}
    if (data['optional'] === true) spec.optional = true
    if (data['type'] !== undefined) {
      spec.type = parseTypeSpecification(data['type'])
    }
    return spec
  }
  return {}
}

// --- Resolution: TypeSpecification -> resolved Type ---

/**
 * Resolve a TypeSpecification against a registry, handling named type
 * lookups and recursive definitions.
 */
export function resolveType(spec: TypeSpecification, registry: TypeRegistry): Type {
  return new Resolver(registry).resolve(spec)
}

class Resolver {
  private seen = new Set<string>()

  constructor(private registry: TypeRegistry) {}

  resolve(spec: TypeSpecification): Type {
    if (spec.name) {
      const found = this.registry.getType(spec.name)
      if (!found) throw new Error(`Type not found: ${spec.name}`)
      this.seen.add(spec.name)
      found.definition = this.resolveDefinition(found.definition)
      return found
    }
    return { name: 'anonymous', definition: this.resolveDefinition(spec) }
  }

  private resolveDefinition(spec: TypeSpecification): TypeSpecification {
    if (spec.base === 'object' || spec.properties) return this.resolveObject(spec)
    if (spec.base === 'array' || spec.listOf) return this.resolveArray(spec)
    if (spec.base) return spec
    throw new Error('Invalid type definition')
  }

  private resolveObject(spec: TypeSpecification): TypeSpecification {
    const base = spec.base ?? 'object'
    const resolvedProps = this.resolveProperties(spec.properties)
    return { base, properties: resolvedProps }
  }

  private resolveArray(spec: TypeSpecification): TypeSpecification {
    const base = spec.base ?? 'array'
    const resolvedListOf = spec.listOf ? this.resolve(spec.listOf).definition : undefined
    return { base, listOf: resolvedListOf }
  }

  private resolveProperties(props?: Record<string, PropertySpecification>): Record<string, PropertySpecification> {
    if (!props) return {}
    const result: Record<string, PropertySpecification> = {}
    for (const [key, prop] of Object.entries(props)) {
      result[key] = this.resolveProperty(prop)
    }
    return result
  }

  private resolveProperty(prop: PropertySpecification): PropertySpecification {
    if (!prop.type) return prop
    if (prop.type.name && this.seen.has(prop.type.name)) {
      // Circular reference — use the already-registered definition
      const found = this.registry.getType(prop.type.name)!
      return { ...prop, type: found.definition }
    }
    const resolved = this.resolve(prop.type).definition
    return { ...prop, type: resolved }
  }
}

// --- Validation: data + resolved TypeSpecification -> error messages ---

/**
 * Validate data against a resolved TypeSpecification.
 * Returns a list of error messages (empty = valid).
 */
export function validateType(data: JsonValue, spec: TypeSpecification): string[] {
  switch (spec.base) {
    case 'string':
      if (!isString(data)) return [`Data should be string but is ${jsonTypeName(data)}`]
      return []
    case 'boolean':
      if (typeof data !== 'boolean') return [`Data should be boolean but is ${jsonTypeName(data)}`]
      return []
    case 'object':
      if (!isObject(data)) return [`Data should be object but is ${jsonTypeName(data)}`]
      return spec.properties ? validateObjectProperties(data, spec.properties) : []
    case 'array':
      if (!isArray(data)) return [`Data should be array but is ${jsonTypeName(data)}`]
      if (spec.listOf) {
        return data.flatMap(item => validateType(item, spec.listOf!))
      }
      return []
    default:
      return []
  }
}

function validateObjectProperties(data: JsonObject, properties: Record<string, PropertySpecification>): string[] {
  const messages: string[] = []
  for (const [field, prop] of Object.entries(properties)) {
    if (field in data) {
      if (prop.type) {
        messages.push(...validateType(data[field], prop.type))
      }
    } else if (!prop.optional) {
      messages.push(`Missing property: ${field}`)
    }
  }
  return messages
}

function jsonTypeName(data: JsonValue): string {
  if (data === null) return 'null'
  if (isArray(data)) return 'array'
  if (isObject(data)) return 'object'
  return typeof data
}

// --- Loading types from types.yaml ---

/**
 * Load types from a parsed types.yaml object into a registry.
 */
export function loadTypes(registry: TypeRegistry, typesData: JsonObject): void {
  for (const [name, definition] of Object.entries(typesData)) {
    const spec = parseTypeSpecification(definition)
    registry.registerType({ name, definition: spec })
  }
}
