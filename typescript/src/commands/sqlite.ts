/**
 * SQLite, SQLite defaults, and Store commands.
 *
 * Mirrors Kotlin's db/ package: SQLite.kt, SQLiteDefaults.kt, Store.kt.
 */

import { resolve } from 'node:path'
import Database from 'better-sqlite3'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, CommandFormatError } from '../language/types.js'
import { parseYamlIfPossible } from '../util/yaml.js'

// --- Session keys ---

const SQLITE_DEFAULTS_KEY = 'sqlite.defaults'

// --- Helpers ---

/** Resolve a database file path relative to the working directory. */
function resolveDbPath(context: ScriptContext, file: string): string {
  return resolve(context.workingDir, file)
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
  const stmt = db.prepare(query)
  const rows = stmt.all() as Record<string, unknown>[]
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

// --- SQLite command ---

export const SQLiteCommand: CommandHandler = {
  name: 'SQLite',

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('SQLite: expected an object')
    }

    // Merge with defaults
    const defaults = context.session.get(SQLITE_DEFAULTS_KEY) as JsonObject | undefined
    const merged = defaults ? { ...defaults, ...data } : data

    const file = merged.file as string | undefined
    if (!file) {
      throw new CommandFormatError('SQLite: missing required parameter: file')
    }

    const update = merged.update as string[] | undefined ?? []
    const query = merged.query as string | undefined
    const dbPath = resolveDbPath(context, file)

    const db = new Database(dbPath)
    try {
      for (const sql of update) {
        doUpdate(db, sql)
      }

      if (query) {
        return doQuery(db, query)
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
      // Multiple columns from json_extract: build an object per column
      for (const col of columns) {
        const obj: JsonObject = {}
        const value = row[col]
        obj[col] = parseYamlIfPossible(value === null || value === undefined ? '' : String(value))
        results.push(obj)
      }
    }
  }

  return results
}
