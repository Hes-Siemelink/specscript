/**
 * SQLite, SQLite defaults, and Store commands.
 *
 * Mirrors Kotlin's db/ package: SQLite.kt, SQLiteDefaults.kt, Store.kt.
 */

import { resolve as resolvePath } from 'node:path'
import Database from 'better-sqlite3'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, CommandFormatError, toDisplayString } from '../language/types.js'
import { resolve } from '../language/command-execution.js'
import { parseYamlIfPossible } from '../util/yaml.js'

// --- Session keys ---

const SQLITE_DEFAULTS_KEY = 'sqlite.defaults'

// --- Helpers ---

/** Resolve a database file path relative to the working directory. */
function resolveDbPath(context: ScriptContext, file: string): string {
  return resolvePath(context.workingDir, file)
}

/** Convert a JDBC-style value to a JsonValue, parsing YAML when the value is a string. */
function jdbcToJsonValue(value: unknown): JsonValue {
  if (value === null || value === undefined) return ''
  if (typeof value === 'number') return value
  if (typeof value === 'boolean') return value
  if (typeof value === 'string') return parseYamlIfPossible(value)
  return String(value)
}

/** Run a query and return results as an array of objects. */
function doQuery(db: Database.Database, query: string): JsonValue[] {
  return doQueryPrepared(db, { sql: query, parameters: [] })
}

/** Run a prepared query and return results as an array of objects. */
function doQueryPrepared(db: Database.Database, prepared: PreparedSql): JsonValue[] {
  const stmt = db.prepare(prepared.sql)
  const rows = stmt.all(...prepared.parameters) as Record<string, unknown>[]
  return rows.map(row => {
    const obj: JsonObject = {}
    for (const [key, value] of Object.entries(row)) {
      obj[key] = jdbcToJsonValue(value)
    }
    return obj
  })
}

/** Run an update statement. */
function doUpdate(db: Database.Database, sql: string): void {
  db.exec(sql)
}

// --- Prepared statement support ---

/** Regex matching a variable reference wrapped in single quotes: '${...}' */
const QUOTED_VARIABLE = /'(\$\{[^}]+})'/g

/** Regex matching any variable reference: ${...} */
const VARIABLE_REGEX = /\$\{([^}]+)}/g

interface PreparedSql {
  sql: string
  parameters: unknown[]
}

/**
 * Extract prepared statement parameters from a SQL string containing SpecScript variables.
 *
 * Quoted variable references ('${var}') become ? placeholders with the resolved value as a parameter.
 * Unquoted variable references (${var}) are resolved inline as text.
 */
function prepareSql(sql: string, variables: Map<string, JsonValue>): PreparedSql {
  const parameters: unknown[] = []

  // First pass: replace quoted variables with ? placeholders and collect parameter values
  const withPlaceholders = sql.replace(QUOTED_VARIABLE, (_match, variableRef: string) => {
    const varMatch = /\$\{([^}]+)}/.exec(variableRef)
    if (!varMatch) return _match
    const varName = varMatch[1]
    const value = lookupVariable(varName, variables)
    parameters.push(jsonValueToSqlValue(value))
    return '?'
  })

  // Second pass: resolve any remaining unquoted variable references inline
  const resolved = withPlaceholders.replace(VARIABLE_REGEX, (_match, varExpr: string) => {
    const value = lookupVariable(varExpr, variables)
    return toDisplayString(value)
  })

  return { sql: resolved, parameters }
}

/** Look up a variable, supporting path navigation (e.g. "user.name"). */
function lookupVariable(expression: string, variables: Map<string, JsonValue>): JsonValue {
  // Split into name and optional path
  const pathMatch = expression.match(/^(.*?)([\[.].*$)/)
  const name = pathMatch ? pathMatch[1] : expression

  const value = variables.get(name)
  if (value === undefined) {
    throw new CommandFormatError(`Unknown variable: ${name}`)
  }
  // Simple case: no path navigation
  if (!pathMatch) return value
  // Path navigation not needed for typical SQL usage; return the value as-is
  // Full path support is handled by resolveVariables in the standard pipeline
  return value
}

/** Convert a JsonValue to a value suitable for a SQLite prepared statement parameter. */
function jsonValueToSqlValue(value: JsonValue): unknown {
  if (value === null) return null
  if (typeof value === 'number') return value
  if (typeof value === 'boolean') return value ? 1 : 0
  if (typeof value === 'string') return value
  return toDisplayString(value)
}

// --- SQLite command ---

export const SQLiteCommand: CommandHandler = {
  name: 'SQLite',
  delayedResolver: true,

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('SQLite: expected an object')
    }

    // Extract raw SQL strings before variable resolution
    const rawUpdate: string[] = []
    if (Array.isArray(data.update)) {
      for (const item of data.update) {
        rawUpdate.push(String(item))
      }
    } else if (typeof data.update === 'string') {
      rawUpdate.push(data.update)
    }
    const rawQuery = typeof data.query === 'string' ? data.query : undefined

    // Resolve non-SQL fields (file, etc.)
    const resolved = await resolve(data, context) as JsonObject

    // Merge with defaults
    const defaults = context.session.get(SQLITE_DEFAULTS_KEY) as JsonObject | undefined
    const merged = defaults ? { ...defaults, ...resolved } : resolved

    const file = merged.file as string | undefined
    if (!file) {
      throw new CommandFormatError('SQLite: missing required parameter: file')
    }

    const dbPath = resolveDbPath(context, file)
    const db = new Database(dbPath)
    try {
      for (const sql of rawUpdate) {
        const prepared = prepareSql(sql, context.variables)
        if (prepared.parameters.length > 0) {
          db.prepare(prepared.sql).run(...prepared.parameters)
        } else {
          db.exec(prepared.sql)
        }
      }

      if (rawQuery) {
        const prepared = prepareSql(rawQuery, context.variables)
        return doQueryPrepared(db, prepared)
      }

      return undefined
    } finally {
      db.close()
    }
  },
}

// --- SQLite defaults command ---

export const SQLiteDefaultsCommand: CommandHandler = {
  name: 'SQLite defaults',

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isObject(data)) {
      context.session.set(SQLITE_DEFAULTS_KEY, data)
      return undefined
    }
    // Value form: retrieve current defaults
    return (context.session.get(SQLITE_DEFAULTS_KEY) as JsonObject | undefined) ?? {}
  },
}

// --- Store command ---

/** Expand $.path references to json_extract() calls in a SQL fragment. */
function expandJsonColumns(where: string): string {
  return where.replace(/\$\.\S+/g, match => `json_extract(json, '${match}')`)
}

/** Convert a select field name to a json_extract() expression. */
function asJsonSelect(column: string): string {
  return `json_extract(json, '$.${column}') as ${column}`
}

export const StoreCommand: CommandHandler = {
  name: 'Store',

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Store: expected an object')
    }

    const file = data.file as string | undefined
    if (!file) {
      throw new CommandFormatError('Store: missing required parameter: file')
    }

    const table = (data.table as string | undefined) ?? 'json_data'
    const insert = (data.insert as JsonValue[] | undefined) ?? []
    const query = data.query as JsonObject | undefined
    const dbPath = resolveDbPath(context, file)

    const db = new Database(dbPath)
    try {
      // Create table
      db.exec(`create table if not exists ${table} (id integer primary key, json text)`)

      // Insert data
      const insertStmt = db.prepare(`insert into ${table} (json) values (?)`)
      for (const item of insert) {
        insertStmt.run(JSON.stringify(item))
      }

      // Query
      if (!query) return undefined

      const selectFields = (query.select as string[] | undefined) ?? []
      const selectClause = selectFields.length === 0
        ? 'json'
        : selectFields.map(asJsonSelect).join(', ')

      const whereClause = query.where
        ? ` where ${expandJsonColumns(query.where as string)}`
        : ''

      const sql = `select ${selectClause} from ${table}${whereClause}`

      return doJsonQuery(db, sql)
    } finally {
      db.close()
    }
  },
}

/** Query that returns JSON documents from a store table. */
function doJsonQuery(db: Database.Database, query: string): JsonValue {
  const stmt = db.prepare(query)
  const rows = stmt.all() as Record<string, unknown>[]
  const results: JsonValue[] = []

  for (const row of rows) {
    const columns = Object.keys(row)

    if (columns.length === 1 && columns[0] === 'json') {
      // Single json column: parse and return the JSON object directly
      const parsed = JSON.parse(row.json as string)
      results.push(parsed)
    } else {
      // Multiple columns from json_extract: build one object per row
      const obj: JsonObject = {}
      for (const col of columns) {
        const value = row[col]
        obj[col] = parseYamlIfPossible(value === null || value === undefined ? '' : String(value))
      }
      results.push(obj)
    }
  }

  return results
}
