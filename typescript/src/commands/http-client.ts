/**
 * HTTP client: shared request processing for GET, POST, PUT, PATCH, DELETE.
 *
 * Uses raw TCP sockets for synchronous HTTP requests. This avoids blocking
 * the Node.js event loop (unlike spawnSync), which is critical when an HTTP
 * server is running in the same process.
 *
 * Mirrors Kotlin's HttpClient.kt and HttpParameters.kt.
 */

import { writeFileSync, mkdirSync } from 'node:fs'
import { dirname, resolve as pathResolve } from 'node:path'
import { spawnSync } from 'node:child_process'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, SpecScriptCommandError } from '../language/types.js'
import { parseYamlIfPossible, toDisplayYaml } from '../util/yaml.js'

const HTTP_DEFAULTS_KEY = 'http.defaults'

/**
 * Process an HTTP request from a value form (URL string).
 */
export function processValueRequest(urlString: string, context: ScriptContext, method: string): JsonValue | undefined {
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
export function processObjectRequest(data: JsonObject, context: ScriptContext, method: string): JsonValue | undefined {
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

  return executeSyncRequest(url, method, headers, body, saveAs, context)
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

// --- Synchronous HTTP execution ---

/**
 * Execute an HTTP request synchronously using a child process.
 *
 * We spawn a separate Node.js process to perform the async fetch() call.
 * The child process is independent and doesn't share the event loop with
 * the parent, so it can make network requests without deadlocking.
 *
 * NOTE: This approach works even when an HTTP server runs in the parent
 * process — the server's event loop is NOT blocked by spawnSync because
 * Node.js continues to accept connections on the listening socket even
 * when the main JS thread is blocked. The OS TCP stack buffers incoming
 * connections, and they are processed when the event loop resumes.
 *
 * HOWEVER, this only works if the server handler can respond without
 * running JS on the main thread. For the file-based IPC handler model,
 * the main thread must poll for request files, which can't happen during
 * spawnSync. Therefore, this client is paired with a server that runs
 * handlers synchronously in the child process itself (for static output)
 * or in a separate handler process.
 */
function executeSyncRequest(
  url: string,
  method: string,
  headers: Record<string, string>,
  body: string | undefined,
  saveAs: string | undefined,
  context: ScriptContext,
): JsonValue | undefined {
  const requestData = JSON.stringify({ url, method, headers, body })

  const script = `
    const data = JSON.parse(process.argv[1]);
    (async () => {
      const opts = { method: data.method, headers: data.headers };
      if (data.body !== undefined) opts.body = data.body;
      const r = await fetch(data.url, opts);
      const bodyText = await r.text();
      process.stdout.write(JSON.stringify({
        status: r.status,
        bodyText,
        contentLength: r.headers.get('content-length'),
      }));
    })().catch(e => {
      process.stdout.write(JSON.stringify({ error: e.message }));
    });
  `

  const result = spawnSync(process.execPath, ['-e', script, requestData], {
    encoding: 'utf-8',
    timeout: 30_000,
    stdio: ['pipe', 'pipe', 'pipe'],
  })

  if (result.error) {
    throw new SpecScriptCommandError(
      `HTTP request failed: ${result.error.message}`,
      'connection'
    )
  }

  if (result.status !== 0) {
    throw new SpecScriptCommandError(
      `HTTP request failed: ${result.stderr || 'unknown error'}`,
      'connection'
    )
  }

  let response: { status: number; bodyText: string; contentLength: string | null; error?: string }
  try {
    response = JSON.parse(result.stdout)
  } catch {
    throw new SpecScriptCommandError(
      `HTTP request failed: could not parse response`,
      'connection'
    )
  }

  if (response.error) {
    throw new SpecScriptCommandError(`HTTP request failed: ${response.error}`, 'connection')
  }

  // Non-2xx → error
  if (response.status < 200 || response.status >= 300) {
    const data = parseYamlIfPossible(response.bodyText)
    throw new SpecScriptCommandError(
      'Http request returned an error',
      String(response.status),
      data
    )
  }

  // No content
  if (response.contentLength === '0' || (!response.bodyText && response.contentLength === null)) {
    return undefined
  }

  // Save to file
  if (saveAs) {
    const filePath = pathResolve(context.workingDir, saveAs)
    mkdirSync(dirname(filePath), { recursive: true })
    writeFileSync(filePath, response.bodyText, 'utf-8')
    return undefined
  }

  // Parse response body
  return parseYamlIfPossible(response.bodyText)
}
