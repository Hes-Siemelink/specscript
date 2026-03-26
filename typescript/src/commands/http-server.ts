/**
 * HTTP server commands: Http server, Http endpoint, Stop http server.
 *
 * Architecture: The HTTP server runs in a forked child process with its own
 * event loop. This prevents deadlocks when the synchronous HTTP client
 * (spawnSync) makes requests to the server.
 *
 * The child process handles request processing:
 * - `output` values: resolved using a minimal variable resolver in the child
 * - `script` handlers (inline or file): delegated to a sub-subprocess that
 *   loads the compiled SpecScript engine from dist/
 *
 * Ready signal: The child writes a file when listening; the parent polls
 * with existsSync (synchronous, no event loop needed).
 *
 * Mirrors Kotlin's HttpServer.kt / HttpEndpoint.kt / StopHttpServer.kt.
 */

import { fork, spawnSync, type ChildProcess } from 'node:child_process'
import { writeFileSync, existsSync, mkdtempSync, unlinkSync, rmSync } from 'node:fs'
import { join, resolve as pathResolve, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isString, SpecScriptError, CommandFormatError } from '../language/types.js'

// --- Server registry ---

interface ManagedServer {
  port: number
  tempDir: string
  serverProcess: ChildProcess
}

const SESSION_KEY_DEFAULT_SERVER = 'http.server.default'

/** Module-level map of running servers by name */
const servers = new Map<string, ManagedServer>()

/**
 * Resolve the dist/ directory path for the compiled SpecScript engine.
 * Used by child process script handlers to spawn engine subprocesses.
 */
function getDistDir(): string {
  // When running from dist/ (compiled), __filename is in dist/commands/
  // When running from src/ via tsx, we need to find dist/ relative to project
  const thisFile = fileURLToPath(import.meta.url)
  const thisDir = dirname(thisFile)

  // Check if we're in dist/commands/ or src/commands/
  const projectDir = pathResolve(thisDir, '..', '..')
  const distDir = join(projectDir, 'dist')
  if (existsSync(join(distDir, 'language', 'script.js'))) {
    return distDir
  }

  // Fallback: we might be running directly from dist/
  const parentDir = pathResolve(thisDir, '..')
  if (existsSync(join(parentDir, 'language', 'script.js'))) {
    return parentDir
  }

  throw new SpecScriptError(
    'Cannot find compiled SpecScript engine (dist/). Run "npx tsc" first.'
  )
}

// --- Http server command ---

export const HttpServerCommand: CommandHandler = {
  name: 'Http server',
  delayedResolver: true,
  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    if (!isObject(data)) {
      throw new CommandFormatError('Http server: expected an object')
    }

    const name = data.name as string
    if (!name) throw new CommandFormatError('Http server: missing required "name" property')
    const port = (data.port as number) ?? 3000

    context.session.set(SESSION_KEY_DEFAULT_SERVER, name)
    ensureRunning(name, port)

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
  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
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
  execute(data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    if (typeof data !== 'string') {
      throw new CommandFormatError('Stop http server: expected a server name string')
    }
    stopServer(data)
    return undefined
  },
}

// --- Child process server script (CommonJS) ---

/**
 * This JS string runs in the forked child process. It:
 * 1. Creates an HTTP server on the specified port
 * 2. Receives route definitions via IPC messages
 * 3. Handles requests by resolving output templates or spawning engine subprocesses
 * 4. Writes a ready-signal file when listening
 */
function buildServerScript(): string {
  // The script is a self-contained CommonJS module
  return `
'use strict';
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const port = parseInt(process.argv[2], 10);
const readyFile = process.argv[3];
const distDir = process.argv[4];

// Route registry: normalizedPath -> Map<METHOD, handlerInfo>
const routes = new Map();

function normalizePath(p) {
  return p.replace(/:(\\w+)/g, '{$1}');
}

function matchRoute(pattern, requestPath) {
  const paramNames = [];
  const regexStr = '^' + pattern.replace(/\\{(\\w+)}/g, (_, name) => {
    paramNames.push(name);
    return '([^/]+)';
  }) + '$';
  const match = new RegExp(regexStr).exec(requestPath);
  if (!match) return null;
  const params = {};
  paramNames.forEach((n, i) => { params[n] = decodeURIComponent(match[i + 1]); });
  return { params };
}

function parseCookies(req) {
  const result = {};
  const header = req.headers.cookie;
  if (!header) return result;
  header.split(';').forEach(pair => {
    const [k, ...rest] = pair.split('=');
    if (k) result[k.trim()] = rest.join('=').trim();
  });
  return result;
}

// Minimal variable resolver for \${...} expressions
function resolveVars(value, vars) {
  if (typeof value === 'string') {
    const fullMatch = /^\\$\\{([^}]+)}$/.exec(value);
    if (fullMatch) {
      return lookupVar(fullMatch[1], vars);
    }
    return value.replace(/\\$\\{([^}]+)}/g, (_, expr) => {
      const v = lookupVar(expr, vars);
      if (v === null || v === undefined) return '';
      if (typeof v === 'object') return JSON.stringify(v);
      return String(v);
    });
  }
  if (Array.isArray(value)) {
    return value.map(item => resolveVars(item, vars));
  }
  if (value !== null && typeof value === 'object') {
    const result = {};
    for (const [k, v] of Object.entries(value)) {
      result[resolveVars(k, vars)] = resolveVars(v, vars);
    }
    return result;
  }
  return value;
}

function lookupVar(expr, vars) {
  const parts = expr.split('.');
  let current = vars[parts[0]];
  for (let i = 1; i < parts.length; i++) {
    if (current === undefined || current === null) return '';
    current = current[parts[i]];
  }
  return current !== undefined ? current : '';
}

/**
 * Run a script handler by spawning a sub-subprocess with the compiled engine.
 * The sub-subprocess imports the engine from dist/, sets up context, and runs
 * the script (inline object or file reference).
 */
function runScriptHandler(scriptData, input, request, scriptDir) {
  // Build an inline ESM script that imports the engine and runs the handler
  const runnerCode = [
    'import { readFileSync } from "node:fs";',
    'import { resolve } from "node:path";',
    'import { Script } from "' + distDir + '/language/script.js";',
    'import { DefaultContext } from "' + distDir + '/language/context.js";',
    'import { registerAllCommands } from "' + distDir + '/commands/register.js";',
    'registerAllCommands();',
    'const d = JSON.parse(process.argv[1]);',
    'const ctx = new DefaultContext({ scriptDir: d.scriptDir || "." });',
    'if (d.input !== undefined) ctx.variables.set("input", d.input);',
    'if (d.request !== undefined) ctx.variables.set("request", d.request);',
    'let result;',
    'if (typeof d.script === "string") {',
    '  const filePath = resolve(d.scriptDir || ".", d.script);',
    '  const content = readFileSync(filePath, "utf-8");',
    '  const s = Script.fromString(content);',
    '  result = s.run(ctx);',
    '} else {',
    '  const s = Script.fromData(d.script);',
    '  result = s.run(ctx);',
    '}',
    'process.stdout.write(JSON.stringify(result ?? null));',
  ].join('\\n');

  const payload = JSON.stringify({ script: scriptData, input, request, scriptDir });

  const result = execFileSync(process.execPath, [
    '--input-type=module', '-e', runnerCode, payload
  ], { encoding: 'utf-8', timeout: 15000, stdio: ['pipe', 'pipe', 'pipe'] });

  if (!result.trim()) return null;
  try { return JSON.parse(result); } catch { return result; }
}

const server = http.createServer((req, res) => {
  const chunks = [];
  req.on('data', c => chunks.push(c));
  req.on('end', () => {
    const bodyText = Buffer.concat(chunks).toString('utf-8');
    const method = (req.method || 'GET').toUpperCase();
    const urlObj = new URL(req.url || '/', 'http://localhost');
    const pathname = urlObj.pathname;

    // Find matching route
    let matched = null;
    let pathParams = {};
    for (const [pattern, methodMap] of routes) {
      const m = matchRoute(pattern, pathname);
      if (m && methodMap.has(method)) {
        matched = methodMap.get(method);
        pathParams = m.params;
        break;
      }
    }

    if (!matched) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Not found' }));
      return;
    }

    // Build headers from rawHeaders (preserves original casing)
    const headers = {};
    for (let i = 0; i < req.rawHeaders.length; i += 2) {
      headers[req.rawHeaders[i]] = req.rawHeaders[i + 1];
    }

    const queryParams = {};
    for (const [k, v] of urlObj.searchParams.entries()) queryParams[k] = v;
    const queryString = urlObj.search ? urlObj.search.substring(1) : '';
    const cookies = parseCookies(req);

    let parsedBody = {};
    if (bodyText.trim()) {
      try { parsedBody = JSON.parse(bodyText); } catch { parsedBody = bodyText; }
    }

    // Build input: body takes precedence over query params
    let input = {};
    if (bodyText.trim()) {
      try { input = JSON.parse(bodyText); } catch { input = bodyText; }
    } else if (Object.keys(queryParams).length > 0) {
      input = queryParams;
    }

    const requestCtx = {
      headers, path: pathname, pathParameters: pathParams,
      query: queryString, queryParameters: queryParams,
      body: parsedBody, cookies,
    };

    const vars = { input, request: requestCtx };

    try {
      let result;

      if (matched.output !== undefined) {
        // Output handler: resolve variables directly
        result = resolveVars(matched.output, vars);
      } else if (matched.script !== undefined) {
        // Script handler (inline or file): delegate to engine subprocess
        result = runScriptHandler(matched.script, input, requestCtx, matched.scriptDir);
      }

      if (result !== undefined && result !== null) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(typeof result === 'string' ? JSON.stringify(result) : JSON.stringify(result));
      } else {
        res.writeHead(200);
        res.end();
      }
    } catch (err) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: err.message || String(err) }));
    }
  });
});

server.listen(port, () => {
  // Signal readiness by writing a file (parent polls with existsSync)
  fs.writeFileSync(readyFile, 'ready');
});

process.on('message', (msg) => {
  if (msg.type === 'add-route') {
    const normalized = normalizePath(msg.path);
    if (!routes.has(normalized)) routes.set(normalized, new Map());
    routes.get(normalized).set(msg.method.toUpperCase(), {
      output: msg.output,
      script: msg.script,
      scriptDir: msg.scriptDir,
    });
  } else if (msg.type === 'stop') {
    server.close(() => process.exit(0));
    // Force exit after a short delay if close doesn't complete
    setTimeout(() => process.exit(0), 100);
  }
});

process.on('SIGTERM', () => {
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(0), 100);
});
`
}

// --- Server lifecycle ---

function ensureRunning(name: string, port: number): ManagedServer {
  const existing = servers.get(name)
  if (existing) return existing

  const tempDir = mkdtempSync(join(tmpdir(), `specscript-http-${name}-`))
  const readyFile = join(tempDir, 'ready')
  const scriptFile = join(tempDir, 'server.js')

  // Resolve dist dir for script handlers
  let distDir: string
  try {
    distDir = getDistDir()
  } catch {
    // If dist/ not available, script handlers will fail but output handlers will work
    distDir = ''
  }

  writeFileSync(scriptFile, buildServerScript())

  const child = fork(scriptFile, [String(port), readyFile, distDir], {
    stdio: ['pipe', 'pipe', 'pipe', 'ipc'],
  })

  const managed: ManagedServer = { port, tempDir, serverProcess: child }
  servers.set(name, managed)

  // Poll for ready signal file (synchronous, no event loop needed)
  const start = Date.now()
  while (!existsSync(readyFile) && Date.now() - start < 10_000) {
    spawnSync(process.execPath, ['-e', ''], { timeout: 5 })
  }

  if (!existsSync(readyFile)) {
    child.kill('SIGTERM')
    servers.delete(name)
    throw new SpecScriptError(`HTTP server '${name}' failed to start on port ${port}`)
  }

  // Clean up ready file
  try { unlinkSync(readyFile) } catch { /* ignore */ }

  return managed
}

export function stopServer(name: string): void {
  const managed = servers.get(name)
  if (!managed) return

  servers.delete(name)

  try {
    if (managed.serverProcess.connected) {
      managed.serverProcess.send({ type: 'stop' })
    }
  } catch {
    // IPC channel already closed
  }

  try {
    managed.serverProcess.kill('SIGTERM')
  } catch {
    // Process already dead
  }

  // Clean up temp directory
  try { rmSync(managed.tempDir, { recursive: true, force: true }) } catch { /* ignore */ }
}

export function stopAllServers(): void {
  for (const name of Array.from(servers.keys())) {
    stopServer(name)
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

    if (managed.serverProcess.connected) {
      managed.serverProcess.send({
        type: 'add-route',
        path: rawPath,
        method,
        output: handlerData.output,
        script: handlerData.script,
        scriptDir: context.scriptDir,
      })
    }
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
