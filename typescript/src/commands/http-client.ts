/**
 * HTTP client: shared request processing for GET, POST, PUT, PATCH, DELETE.
 *
 * Uses native async fetch() for HTTP requests. Now that the engine is async,
 * we can use fetch() directly without spawning child processes.
 *
 * Mirrors Kotlin's HttpClient.kt and HttpParameters.kt.
 */

import { writeFileSync, mkdirSync } from 'node:fs'
import { dirname, resolve as pathResolve } from 'node:path'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, SpecScriptCommandError } from '../language/types.js'
import { parseYamlIfPossible, toDisplayYaml } from '../util/yaml.js'

const HTTP_DEFAULTS_KEY = 'http.defaults'

/**
 * Process an HTTP request from a value form (URL string).
 */
export async function processValueRequest(urlString: string, context: ScriptContext, method: string): Promise<JsonValue | undefined> {
  const encoded = encodePath(urlString)

  let data: JsonObject
  try {
    const uri = new URL(encoded)
    const pathStart = encoded.indexOf(uri.pathname)
    const url = encoded.substring(0, pathStart)
    const path = encoded.substring(pathStart)
    data = url ? { url, path } : { path }
  } catch {
    // Not a full URL — treat entire string as path (needs defaults for url)
    data = { path: encoded }
  }

  return processObjectRequest(data, context, method)
}

/**
 * Process an HTTP request from an object form.
 */
export async function processObjectRequest(data: JsonObject, context: ScriptContext, method: string): Promise<JsonValue | undefined> {
  const defaults = getDefaults(context)
  const merged = mergeWithDefaults({ ...data }, defaults)

  const url = buildUrl(merged)
  const headers = buildHeaders(merged)

  // Add cookies to headers
  addCookieHeader(merged, headers)

  // Add basic auth
  if (merged.username) {
    const username = String(merged.username)
    const password = String(merged.password ?? '')
    headers['Authorization'] = `Basic ${Buffer.from(`${username}:${password}`).toString('base64')}`
  }

  const body = buildBody(merged, headers)
  const saveAs = merged['save as'] as string | undefined

  return executeAsyncRequest(url, method, headers, body, saveAs, context)
}

// --- Defaults management ---

export function storeDefaults(context: ScriptContext, data: JsonObject): void {
  context.session.set(HTTP_DEFAULTS_KEY, data)
}

export function getDefaults(context: ScriptContext): JsonObject | undefined {
  return context.session.get(HTTP_DEFAULTS_KEY) as JsonObject | undefined
}

// --- Parameter merging ---

function mergeWithDefaults(data: JsonObject, defaults: JsonObject | undefined): JsonObject {
  if (!defaults) return data

  for (const [key, value] of Object.entries(defaults)) {
    if (!(key in data)) {
      data[key] = value
    } else if (isObject(data[key]) && isObject(value)) {
      mergeWithDefaults(data[key] as JsonObject, value as JsonObject)
    }
  }
  return data
}

// --- URL building ---

function buildUrl(params: JsonObject): string {
  const host = (params.url as string) ?? ''
  const path = encodePath(params.path as string | undefined)
  return `${host}${path}`
}

function encodePath(path: string | null | undefined): string {
  if (!path) return ''
  return path.replace(/ /g, '+')
}

// --- Headers ---

function buildHeaders(params: JsonObject): Record<string, string> {
  const headers: Record<string, string> = {}

  const paramHeaders = params.headers
  if (isObject(paramHeaders)) {
    for (const [key, value] of Object.entries(paramHeaders)) {
      headers[key] = String(value)
    }
  }

  // Default Content-Type if not set
  if (!Object.keys(headers).some(k => k.toLowerCase() === 'content-type')) {
    headers['Content-Type'] = 'application/json'
  }

  // Default Accept if not set
  if (!Object.keys(headers).some(k => k.toLowerCase() === 'accept')) {
    headers['Accept'] = '*/*'
  }

  return headers
}

// --- Cookies ---

function addCookieHeader(params: JsonObject, headers: Record<string, string>): void {
  const paramCookies = params.cookies
  if (!isObject(paramCookies)) return

  const cookiePairs = Object.entries(paramCookies).map(([k, v]) => `${k}=${v}`)
  if (cookiePairs.length > 0) {
    headers['Cookie'] = cookiePairs.join('; ')
  }
}

// --- Body ---

function buildBody(params: JsonObject, headers: Record<string, string>): string | undefined {
  const body = params.body
  if (body === undefined || body === null) return undefined

  const contentType = Object.entries(headers).find(
    ([k]) => k.toLowerCase() === 'content-type'
  )?.[1] ?? ''

  if (contentType === 'application/x-www-form-urlencoded') {
    if (isObject(body)) {
      return Object.entries(body)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(toDisplayYaml(v as JsonValue))}`)
        .join('&')
    }
  }

  return JSON.stringify(body)
}

// --- Async HTTP execution ---

/**
 * Execute an HTTP request using native async fetch().
 * Now that the engine is async, we can use fetch() directly.
 */
async function executeAsyncRequest(
  url: string,
  method: string,
  headers: Record<string, string>,
  body: string | undefined,
  saveAs: string | undefined,
  context: ScriptContext,
): Promise<JsonValue | undefined> {
  const opts: RequestInit = { method, headers }
  if (body !== undefined) opts.body = body

  let response: Response
  try {
    response = await fetch(url, opts)
  } catch (e) {
    throw new SpecScriptCommandError(
      `HTTP request failed: ${e instanceof Error ? e.message : String(e)}`,
      'connection'
    )
  }

  const bodyText = await response.text()

  // Non-2xx → error
  if (response.status < 200 || response.status >= 300) {
    const data = parseYamlIfPossible(bodyText)
    throw new SpecScriptCommandError(
      'Http request returned an error',
      String(response.status),
      data
    )
  }

  // No content
  const contentLength = response.headers.get('content-length')
  if (contentLength === '0' || (!bodyText && contentLength === null)) {
    return undefined
  }

  // Save to file
  if (saveAs) {
    const filePath = pathResolve(context.workingDir, saveAs)
    mkdirSync(dirname(filePath), { recursive: true })
    writeFileSync(filePath, bodyText, 'utf-8')
    return undefined
  }

  // Parse response body
  return parseYamlIfPossible(bodyText)
}
