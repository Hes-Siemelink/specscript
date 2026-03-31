/**
 * HTTP server commands: Http server, Http endpoint, Stop http server.
 *
 * Architecture: Now that the engine is async, the HTTP server runs in-process
 * using Node.js's native http.createServer(). Request handlers run inline —
 * no child processes or IPC needed.
 *
 * Mirrors Kotlin's HttpServer.kt / HttpEndpoint.kt / StopHttpServer.kt.
 */

import { createServer, type Server, type IncomingMessage, type ServerResponse } from 'node:http'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isString, SpecScriptError, CommandFormatError } from '../language/types.js'
import { Script } from '../language/script.js'
import { DefaultContext } from '../language/context.js'
import { setupSilentCapture } from '../language/stdout-capture.js'
import { resolveVariables } from '../language/variables.js'

// --- Server registry ---

interface RouteHandler {
  output?: JsonValue
  script?: JsonValue
  scriptDir: string
  parentContext: ScriptContext
}

interface ManagedServer {
  port: number
  server: Server
  routes: Map<string, Map<string, RouteHandler>>  // normalizedPath -> Map<METHOD, handler>
}

const SESSION_KEY_DEFAULT_SERVER = 'http.server.default'

/** Module-level map of running servers by name */
const servers = new Map<string, ManagedServer>()

// --- Http server command ---

export const HttpServerCommand: CommandHandler = {
  name: 'Http server',
  delayedResolver: true,
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Http server: expected an object')
    }

    const name = data.name as string
    if (!name) throw new CommandFormatError('Http server: missing required "name" property')
    const port = (data.port as number) ?? 3000

    context.session.set(SESSION_KEY_DEFAULT_SERVER, name)
    await ensureRunning(name, port, context)

    const endpoints = data.endpoints
    if (isObject(endpoints)) {
      for (const [path, endpointData] of Object.entries(endpoints)) {
        if (isObject(endpointData)) {
          installEndpointOnServer(servers.get(name)!, path, endpointData, context)
        }
      }
    }

    return undefined
  },
}

// --- Http endpoint command ---

export const HttpEndpointCommand: CommandHandler = {
  name: 'Http endpoint',
  delayedResolver: true,
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Http endpoint: expected an object')
    }

    const serverName = getDefaultServerName(context)
    const managed = servers.get(serverName)
    if (!managed) {
      throw new SpecScriptError(
        `HTTP server '${serverName}' is not running. Start it with Http server before adding endpoints.`
      )
    }

    for (const [path, endpointData] of Object.entries(data)) {
      if (isObject(endpointData)) {
        installEndpointOnServer(managed, path, endpointData, context)
      }
    }

    return undefined
  },
}

// --- Stop http server command ---

export const StopHttpServerCommand: CommandHandler = {
  name: 'Stop http server',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (typeof data !== 'string') {
      throw new CommandFormatError('Stop http server: expected a server name string')
    }
    await stopServer(data, context)
    return undefined
  },
}

// --- Path matching ---

function normalizePath(p: string): string {
  return p.replace(/:(\w+)/g, '{$1}')
}

function matchRoute(pattern: string, requestPath: string): { params: Record<string, string> } | null {
  const paramNames: string[] = []
  const regexStr = '^' + pattern.replace(/\{(\w+)}/g, (_, name) => {
    paramNames.push(name)
    return '([^/]+)'
  }) + '$'
  const match = new RegExp(regexStr).exec(requestPath)
  if (!match) return null
  const params: Record<string, string> = {}
  paramNames.forEach((n, i) => { params[n] = decodeURIComponent(match[i + 1]) })
  return { params }
}

function parseCookies(req: IncomingMessage): Record<string, string> {
  const result: Record<string, string> = {}
  const header = req.headers.cookie
  if (!header) return result
  header.split(';').forEach(pair => {
    const [k, ...rest] = pair.split('=')
    if (k) result[k.trim()] = rest.join('=').trim()
  })
  return result
}

// --- Server lifecycle ---

function writeToContext(context: ScriptContext | undefined, text: string): void {
  const writer = context?.session.get('stdout') as ((s: string) => void) | undefined
  if (writer) writer(text)
  else console.log(text)
}

async function ensureRunning(name: string, port: number, context: ScriptContext): Promise<ManagedServer> {
  const existing = servers.get(name)
  if (existing) return existing

  const routes = new Map<string, Map<string, RouteHandler>>()

  const server = createServer(async (req: IncomingMessage, res: ServerResponse) => {
    const chunks: Buffer[] = []
    req.on('data', (c: Buffer) => chunks.push(c))
    req.on('end', async () => {
      try {
        await handleRequest(req, res, chunks, routes)
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' })
        res.end(JSON.stringify({ error: err instanceof Error ? err.message : String(err) }))
      }
    })
  })

  const managed: ManagedServer = { port, server, routes }
  servers.set(name, managed)

  // Start listening and wait for the server to be ready
  await new Promise<void>((resolve, reject) => {
    server.on('error', reject)
    server.listen(port, () => resolve())
  })

  writeToContext(context, `Starting SpecScript Http Server '${name}' on port ${port}`)
  return managed
}

async function handleRequest(
  req: IncomingMessage,
  res: ServerResponse,
  chunks: Buffer[],
  routes: Map<string, Map<string, RouteHandler>>
): Promise<void> {
  const bodyText = Buffer.concat(chunks).toString('utf-8')
  const method = (req.method || 'GET').toUpperCase()
  const urlObj = new URL(req.url || '/', 'http://localhost')
  const pathname = urlObj.pathname

  // Find matching route
  let matched: RouteHandler | null = null
  let pathParams: Record<string, string> = {}
  for (const [pattern, methodMap] of routes) {
    const m = matchRoute(pattern, pathname)
    if (m && methodMap.has(method)) {
      matched = methodMap.get(method)!
      pathParams = m.params
      break
    }
  }

  if (!matched) {
    res.writeHead(404, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ error: 'Not found' }))
    return
  }

  // Build headers from rawHeaders (preserves original casing)
  const headers: Record<string, string> = {}
  for (let i = 0; i < req.rawHeaders.length; i += 2) {
    headers[req.rawHeaders[i]] = req.rawHeaders[i + 1]
  }

  const queryParams: Record<string, string> = {}
  for (const [k, v] of urlObj.searchParams.entries()) queryParams[k] = v
  const queryString = urlObj.search ? urlObj.search.substring(1) : ''
  const cookies = parseCookies(req)

  let parsedBody: JsonValue = {}
  if (bodyText.trim()) {
    try { parsedBody = JSON.parse(bodyText) } catch { parsedBody = bodyText }
  }

  // Build input: body takes precedence over query params
  let input: JsonValue = {}
  if (bodyText.trim()) {
    try { input = JSON.parse(bodyText) } catch { input = bodyText }
  } else if (Object.keys(queryParams).length > 0) {
    input = queryParams
  }

  const requestCtx: JsonObject = {
    headers, path: pathname, pathParameters: pathParams,
    query: queryString, queryParameters: queryParams,
    body: parsedBody, cookies,
  }

  try {
    let result: JsonValue | undefined

    if (matched.output !== undefined) {
      // Output handler: resolve variables
      const vars = new Map<string, JsonValue>()
      vars.set('input', input)
      vars.set('request', requestCtx)
      result = resolveVariables(matched.output, vars)
    } else if (matched.script !== undefined) {
      // Script handler: run in-process
      result = await runScriptHandler(matched.script, input, requestCtx, matched.scriptDir, matched.parentContext)
    }

    if (result !== undefined && result !== null) {
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(typeof result === 'string' ? JSON.stringify(result) : JSON.stringify(result))
    } else {
      res.writeHead(200)
      res.end()
    }
  } catch (err) {
    res.writeHead(500, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ error: err instanceof Error ? err.message : String(err) }))
  }
}

/**
 * Run a script handler in-process.
 * Creates a fresh context and runs the script directly.
 */
async function runScriptHandler(
  scriptData: JsonValue,
  input: JsonValue,
  request: JsonObject,
  scriptDir: string,
  parentContext: ScriptContext
): Promise<JsonValue | undefined> {
  const ctx = new DefaultContext({
    scriptFile: scriptDir + '/handler.spec.yaml',
    workingDir: scriptDir,
    session: parentContext.session,
  })
  ctx.variables.set('input', input)
  ctx.variables.set('request', request)
  setupSilentCapture(ctx)

  if (isString(scriptData)) {
    // Script file reference
    const { readFileSync } = await import('node:fs')
    const { resolve } = await import('node:path')
    const filePath = resolve(scriptDir, scriptData)
    const content = readFileSync(filePath, 'utf-8')
    const script = Script.fromString(content)
    return script.run(ctx)
  } else {
    // Inline script object
    const script = Script.fromData(scriptData)
    return script.run(ctx)
  }
}

export async function stopServer(name: string, context?: ScriptContext): Promise<void> {
  const managed = servers.get(name)
  if (!managed) return

  writeToContext(context, `Stopping SpecScript Http Server '${name}'`)
  servers.delete(name)

  await new Promise<void>((resolve) => {
    managed.server.close(() => resolve())
    // Force-close after a short delay
    setTimeout(() => resolve(), 100)
  })
}

export async function stopAllServers(): Promise<void> {
  const names = Array.from(servers.keys())
  for (const name of names) {
    await stopServer(name)
  }
}

function getDefaultServerName(context: ScriptContext): string {
  const name = context.session.get(SESSION_KEY_DEFAULT_SERVER) as string | undefined
  if (!name) {
    throw new SpecScriptError(
      'No HTTP server found in current context. An Http server must be started before defining endpoints.'
    )
  }
  return name
}

// --- Endpoint installation ---

function installEndpointOnServer(
  managed: ManagedServer,
  rawPath: string,
  endpointData: JsonObject,
  context: ScriptContext,
): void {
  for (const [methodName, handlerValue] of Object.entries(endpointData)) {
    const method = methodName.toUpperCase()
    if (!SUPPORTED_METHODS.has(method)) {
      throw new CommandFormatError(`Unsupported HTTP method: ${methodName}`)
    }

    const handlerData = parseHandlerData(handlerValue as JsonValue)
    const normalized = normalizePath(rawPath)

    if (!managed.routes.has(normalized)) {
      managed.routes.set(normalized, new Map())
    }
    managed.routes.get(normalized)!.set(method, {
      output: handlerData.output,
      script: handlerData.script,
      scriptDir: context.scriptDir,
      parentContext: context,
    })
  }
}

const SUPPORTED_METHODS = new Set(['GET', 'POST', 'PUT', 'PATCH', 'DELETE'])

interface HandlerData {
  output?: JsonValue
  script?: JsonValue
}

function parseHandlerData(value: JsonValue): HandlerData {
  if (isString(value)) {
    // String value = script file reference (like "handlers/create-greeting.spec.yaml")
    return { script: value }
  }
  if (isObject(value)) {
    if (value.output !== undefined) {
      return { output: value.output }
    }
    if (value.script !== undefined) {
      return { script: value.script }
    }
    // Object without output/script key — treat as inline output
    return { output: value }
  }
  // Primitive value — treat as static output
  return { output: value }
}
