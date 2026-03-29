import { SpecScriptError } from './types.js'
import type { JsonValue, JsonObject } from './types.js'

export interface PackageImport {
  source: string
  items: ImportItem[]
  local: boolean
}

export type ImportItem =
  | { kind: 'command'; path: string; name: string; alias?: string }
  | { kind: 'name'; value: string; alias?: string }
  | { kind: 'directory'; path: string }
  | { kind: 'wildcard'; path: string; recursive: boolean }

export function parseImports(imports: JsonValue | undefined): PackageImport[] {
  if (imports === undefined || imports === null) return []

  if (Array.isArray(imports)) {
    throw new SpecScriptError(
      'imports must be a map, not a list. See the Packages specification for the correct format.'
    )
  }

  if (typeof imports !== 'object') {
    throw new SpecScriptError('imports must be a map')
  }

  const result: PackageImport[] = []
  for (const [source, value] of Object.entries(imports as JsonObject)) {
    const local = source.startsWith('./')
    const items = parseImportItems(value)
    result.push({ source, items, local })
  }
  return result
}

function parseImportItems(node: JsonValue): ImportItem[] {
  if (node === null || node === undefined) {
    return [{ kind: 'wildcard', path: '', recursive: false }]
  }

  if (typeof node === 'string') {
    return [parseImportString(node)]
  }

  if (Array.isArray(node)) {
    return node.map(element => parseElement(element))
  }

  throw new SpecScriptError('Import value must be a string or list')
}

function parseImportString(value: string): ImportItem {
  if (value === '*') return { kind: 'wildcard', path: '', recursive: false }
  if (value === '**') return { kind: 'wildcard', path: '', recursive: true }
  if (value.endsWith('/*')) return { kind: 'wildcard', path: value.slice(0, -2), recursive: false }
  if (value.endsWith('/**')) return { kind: 'wildcard', path: value.slice(0, -3), recursive: true }

  const slashIndex = value.lastIndexOf('/')
  if (slashIndex > 0) {
    const commandName = value.substring(slashIndex + 1)
    return { kind: 'command', path: value, name: commandName }
  }

  return { kind: 'name', value }
}

function parseElement(node: JsonValue): ImportItem {
  if (typeof node === 'string') {
    return parseImportString(node)
  }

  if (node !== null && typeof node === 'object' && !Array.isArray(node)) {
    const entries = Object.entries(node as JsonObject)
    if (entries.length === 0) throw new SpecScriptError('Import item map must not be empty')
    const [key, value] = entries[0]
    const alias = (value !== null && typeof value === 'object' && !Array.isArray(value))
      ? (value as JsonObject)['as'] as string | undefined
      : undefined
    return parseWithAlias(key, alias)
  }

  throw new SpecScriptError('Import item must be a string or map')
}

function parseWithAlias(key: string, alias: string | undefined): ImportItem {
  const slashIndex = key.lastIndexOf('/')
  if (slashIndex > 0) {
    const commandName = key.substring(slashIndex + 1)
    return { kind: 'command', path: key, name: commandName, alias }
  }

  if (alias) {
    return { kind: 'name', value: key, alias }
  }

  return { kind: 'directory', path: key }
}
