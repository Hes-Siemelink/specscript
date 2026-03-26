import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isArray, isString } from '../language/types.js'
import { deepEquals } from '../language/conditions.js'

/**
 * Apply a JSON Patch (RFC 6902) to a document.
 * Returns a new document with the patches applied.
 */
export function applyJsonPatch(doc: JsonValue, patch: JsonValue[]): JsonValue {
  let result = JSON.parse(JSON.stringify(doc)) // deep copy

  for (const operation of patch) {
    if (!isObject(operation)) {
      throw new Error(`Invalid operation: ${JSON.stringify(operation)}`)
    }
    result = performOperation(operation, result)
  }

  return result
}

function performOperation(operation: JsonObject, doc: JsonValue): JsonValue {
  const op = operation['op']
  if (!isString(op)) {
    throw new Error(`Invalid "op" property: ${JSON.stringify(op)}`)
  }
  const path = operation['path']
  if (!isString(path)) {
    throw new Error(`Invalid "path" property: ${JSON.stringify(path)}`)
  }
  if (path.length > 0 && path[0] !== '/') {
    throw new Error(`Invalid "path" property: ${path}`)
  }

  switch (op) {
    case 'add': {
      const value = operation['value']
      if (value === undefined) throw new Error('Missing "value" property')
      return patchAdd(doc, path, value)
    }
    case 'remove':
      return patchRemove(doc, path)
    case 'replace': {
      const value = operation['value']
      if (value === undefined) throw new Error('Missing "value" property')
      return patchReplace(doc, path, value)
    }
    case 'move': {
      const from = operation['from']
      if (!isString(from)) throw new Error(`Invalid "from" property: ${JSON.stringify(from)}`)
      if (from.length > 0 && from[0] !== '/') throw new Error(`Invalid "from" property: ${from}`)
      return patchMove(doc, path, from)
    }
    case 'copy': {
      const from = operation['from']
      if (!isString(from)) throw new Error(`Invalid "from" property: ${JSON.stringify(from)}`)
      if (from.length > 0 && from[0] !== '/') throw new Error(`Invalid "from" property: ${from}`)
      return patchCopy(doc, path, from)
    }
    case 'test': {
      const value = operation['value']
      if (value === undefined) throw new Error('Missing "value" property')
      return patchTest(doc, path, value)
    }
    default:
      throw new Error(`Invalid "op" property: ${op}`)
  }
}

/** Split a JSON Pointer path into segments, handling ~ escaping */
function splitPath(path: string): string[] {
  if (path === '' || path === '/') return path === '/' ? [''] : []
  return path.substring(1).split('/').map(s =>
    s.replace(/~1/g, '/').replace(/~0/g, '~')
  )
}

/** Get the parent container and the last segment */
function getParent(doc: JsonValue, path: string): { parent: JsonValue; key: string } {
  const segments = splitPath(path)
  const key = segments[segments.length - 1]
  const parentSegments = segments.slice(0, -1)

  let parent: JsonValue = doc
  for (const seg of parentSegments) {
    if (isArray(parent)) {
      const idx = parseInt(seg, 10)
      parent = parent[idx]
    } else if (isObject(parent)) {
      parent = parent[seg]
    } else {
      throw new Error(`Invalid "path" property: ${path}`)
    }
  }

  return { parent, key }
}

/** Navigate to a node by JSON Pointer */
function getNode(doc: JsonValue, path: string): JsonValue | undefined {
  if (path === '') return doc
  const segments = splitPath(path)
  let current: JsonValue = doc
  for (const seg of segments) {
    if (isArray(current)) {
      const idx = parseInt(seg, 10)
      if (isNaN(idx) || idx < 0 || idx >= current.length) return undefined
      current = current[idx]
    } else if (isObject(current)) {
      if (!(seg in current)) return undefined
      current = current[seg]
    } else {
      return undefined
    }
  }
  return current
}

function patchAdd(doc: JsonValue, path: string, value: JsonValue): JsonValue {
  if (path === '') return value

  const { parent, key } = getParent(doc, path)

  if (isObject(parent)) {
    ;(parent as JsonObject)[key] = value
  } else if (isArray(parent)) {
    if (key === '-') {
      parent.push(value)
    } else {
      const idx = parseInt(key, 10)
      if (isNaN(idx) || idx > parent.length || idx < 0) {
        throw new Error(`Array index is out of bounds: ${idx}`)
      }
      parent.splice(idx, 0, value)
    }
  } else {
    throw new Error(`Invalid "path" property: ${path}`)
  }

  return doc
}

function patchRemove(doc: JsonValue, path: string): JsonValue {
  if (path === '') {
    if (isObject(doc)) return {}
    if (isArray(doc)) return []
    throw new Error(`Invalid "path" property: ${path}`)
  }

  const { parent, key } = getParent(doc, path)

  if (isObject(parent)) {
    if (!(key in (parent as JsonObject))) {
      throw new Error(`Property does not exist: ${key}`)
    }
    delete (parent as JsonObject)[key]
  } else if (isArray(parent)) {
    const idx = parseInt(key, 10)
    if (isNaN(idx) || idx < 0 || idx >= parent.length) {
      throw new Error(`Index does not exist: ${key}`)
    }
    parent.splice(idx, 1)
  } else {
    throw new Error(`Invalid "path" property: ${path}`)
  }

  return doc
}

function patchReplace(doc: JsonValue, path: string, value: JsonValue): JsonValue {
  const stripped = patchRemove(doc, path)
  return patchAdd(stripped, path, value)
}

function patchMove(doc: JsonValue, path: string, from: string): JsonValue {
  const value = getNode(doc, from)
  if (value === undefined) throw new Error(`Invalid "from" property: ${from}`)
  const stripped = patchRemove(doc, from)
  return patchAdd(stripped, path, JSON.parse(JSON.stringify(value)))
}

function patchCopy(doc: JsonValue, path: string, from: string): JsonValue {
  const value = getNode(doc, from)
  if (value === undefined) throw new Error(`Invalid "from" property: ${from}`)
  return patchAdd(doc, path, JSON.parse(JSON.stringify(value)))
}

function patchTest(doc: JsonValue, path: string, value: JsonValue): JsonValue {
  const node = getNode(doc, path)
  if (node === undefined) throw new Error(`Invalid "path" property: ${path}`)
  if (!deepEquals(node, value)) {
    throw new Error('The value does not equal path node')
  }
  return doc
}
