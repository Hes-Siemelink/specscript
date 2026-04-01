/**
 * Credentials subsystem: storage, CRUD commands, and the Credentials config command.
 *
 * Mirrors Kotlin's specscript/commands/connections/ package.
 */

import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { homedir } from 'node:os'
import { stringify } from 'yaml'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isString, isObject, SpecScriptCommandError, CommandFormatError } from '../language/types.js'
import { parseYaml } from '../util/yaml.js'

// --- Data model ---

interface TargetResource {
  credentials: JsonObject[]
  default?: string
}

interface CredentialsFile {
  filePath: string | undefined
  targetResources: Record<string, TargetResource>
}

function getDefaultCredential(target: TargetResource): JsonObject | undefined {
  if (!target.default) return undefined
  return target.credentials.find(c => c.name === target.default)
}

// --- Storage ---

const CREDENTIALS_SESSION_KEY = 'credentials.yaml'
const DEFAULT_DIR = join(homedir(), '.specscript')
const DEFAULT_FILE = join(DEFAULT_DIR, 'credentials.yaml')

function loadCredentialsFile(filePath: string): CredentialsFile {
  const targetResources: Record<string, TargetResource> = {}

  if (existsSync(filePath)) {
    const content = readFileSync(filePath, 'utf-8')
    const parsed = parseYaml(content)
    if (parsed && isObject(parsed)) {
      for (const [name, value] of Object.entries(parsed)) {
        if (isObject(value)) {
          const credentials = Array.isArray(value.credentials) ? value.credentials as JsonObject[] : []
          const defaultName = isString(value.default) ? value.default : undefined
          targetResources[name] = { credentials, default: defaultName }
        }
      }
    }
  } else {
    const dir = filePath.substring(0, filePath.lastIndexOf('/'))
    if (dir && !existsSync(dir)) {
      mkdirSync(dir, { recursive: true })
    }
    writeFileSync(filePath, '', 'utf-8')
  }

  return { filePath, targetResources }
}

function saveCredentials(credentials: CredentialsFile): void {
  if (!credentials.filePath) {
    throw new Error("Can't save Credentials object because there is no file associated with it.")
  }
  const yaml = stringify(credentials.targetResources, { lineWidth: 0, minContentWidth: 0 })
  writeFileSync(credentials.filePath, yaml, 'utf-8')
}

function getCredentials(context: ScriptContext): CredentialsFile {
  let creds = context.session.get(CREDENTIALS_SESSION_KEY) as CredentialsFile | undefined
  if (!creds) {
    creds = loadCredentialsFile(DEFAULT_FILE)
    context.session.set(CREDENTIALS_SESSION_KEY, creds)
  }
  return creds
}

function setCredentials(context: ScriptContext, credentials: CredentialsFile): void {
  context.session.set(CREDENTIALS_SESSION_KEY, credentials)
}

// --- Commands ---

/** Credentials command: set the active credentials file path. */
export const CredentialsCommand: CommandHandler = {
  name: 'Credentials',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isString(data)) {
      throw new CommandFormatError('Credentials: expected a file path string')
    }
    const filePath = resolve(data)
    const credentials = loadCredentialsFile(filePath)
    setCredentials(context, credentials)
    return undefined
  },
}

/** Get all credentials for a target. */
export const GetAllCredentialsCommand: CommandHandler = {
  name: 'Get all credentials',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isString(data)) {
      throw new CommandFormatError('Get all credentials: expected a target name string')
    }
    const credentials = getCredentials(context)
    const target = credentials.targetResources[data]
    if (!target) {
      throw new SpecScriptCommandError(
        `Unknown target ${data}`,
        'unknown target',
        { target: data },
      )
    }
    return [...target.credentials]
  },
}

/** Get the default (or first) credential for a target. */
export const GetCredentialsCommand: CommandHandler = {
  name: 'Get credentials',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isString(data)) {
      throw new CommandFormatError('Get credentials: expected a target name string')
    }
    const credentials = getCredentials(context)
    const target = credentials.targetResources[data]
    if (!target) return ''

    if (target.default != null) {
      return getDefaultCredential(target) ?? null
    }

    if (target.credentials.length > 0) {
      return target.credentials[0]
    }

    throw new SpecScriptCommandError(
      `No accounts defined for ${data}`,
      'no accounts',
      { target: data },
    )
  },
}

/** Set the default credential for a target. */
export const SetDefaultCredentialsCommand: CommandHandler = {
  name: 'Set default credentials',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Set default credentials: expected an object with target and name')
    }
    const targetName = data.target as string
    const newDefault = data.name as string
    const credentials = getCredentials(context)
    const target = credentials.targetResources[targetName]
    if (!target) return undefined

    target.default = newDefault
    saveCredentials(credentials)
    return undefined
  },
}

/** Create a new credential entry for a target. */
export const CreateCredentialsCommand: CommandHandler = {
  name: 'Create credentials',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Create credentials: expected an object with target and credentials')
    }
    const targetName = isString(data.target) ? data.target : 'Default'
    const newCredential = (isObject(data.credentials) ? data.credentials : {}) as JsonObject
    const credentials = getCredentials(context)

    if (!credentials.targetResources[targetName]) {
      credentials.targetResources[targetName] = { credentials: [] }
    }
    credentials.targetResources[targetName].credentials.push(newCredential)
    saveCredentials(credentials)

    return newCredential
  },
}

/** Delete a credential entry from a target. */
export const DeleteCredentialsCommand: CommandHandler = {
  name: 'Delete credentials',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Delete credentials: expected an object with target and name')
    }
    const targetName = data.target as string
    const credentialName = data.name as string
    const credentials = getCredentials(context)
    const target = credentials.targetResources[targetName]
    if (!target) return undefined

    target.credentials = target.credentials.filter(c => c.name !== credentialName)
    saveCredentials(credentials)
    return undefined
  },
}
