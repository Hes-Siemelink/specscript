/**
 * MCP server commands: Mcp server, Mcp tool, Mcp resource, Mcp prompt,
 * Mcp call tool, Mcp read resource, Mcp get prompt, Stop mcp server.
 *
 * Mirrors Kotlin's commands/mcp/ package. Uses the low-level Server class from
 * @modelcontextprotocol/sdk for protocol handling, because the high-level
 * McpServer class requires Zod schemas whereas SpecScript supplies raw JSON
 * Schema objects.
 */

import { createServer, type Server as HttpServer, type IncomingMessage, type ServerResponse } from 'node:http'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { Server as McpLowLevelServer } from '@modelcontextprotocol/sdk/server/index.js'
import {
  ListToolsRequestSchema, CallToolRequestSchema,
  ListResourcesRequestSchema, ReadResourceRequestSchema,
  ListPromptsRequestSchema, GetPromptRequestSchema,
} from '@modelcontextprotocol/sdk/types.js'
import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js'
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js'
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'
import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isString, SpecScriptError, SpecScriptCommandError, CommandFormatError } from '../language/types.js'
import { Script } from '../language/script.js'
import { DefaultContext } from '../language/context.js'
import { setupSilentCapture } from '../language/stdout-capture.js'
import { resolveVariables } from '../language/variables.js'
import { toDisplayYaml, parseYamlIfPossible, parseYamlCommands } from '../util/yaml.js'

// --- Types ---

interface ToolEntry {
  name: string
  description: string
  inputSchema: RawJsonSchema
  handler: { output?: JsonValue; script?: JsonValue }
  context: ScriptContext
}

interface RawJsonSchema {
  type: string
  properties?: JsonObject
  required?: string[]
}

interface ResourceEntry {
  uri: string
  name: string
  description: string
  mimeType: string
  handler: { output?: JsonValue; script?: JsonValue }
  context: ScriptContext
}

interface PromptEntry {
  name: string
  description: string
  arguments: PromptArgumentInfo[]
  handler: { output?: JsonValue; script?: JsonValue }
  context: ScriptContext
}

interface PromptArgumentInfo {
  name: string
  description?: string
  required?: boolean
}

type TransportType = 'STDIO' | 'HTTP'

// --- Server registry ---

const SESSION_KEY_DEFAULT_SERVER = 'mcp.server.default'

interface McpManagedServer {
  server: McpLowLevelServer
  serverName: string
  serverVersion: string
  httpServer?: HttpServer
  transportType: TransportType
  tools: ToolEntry[]
  resources: ResourceEntry[]
  prompts: PromptEntry[]
}

const servers = new Map<string, McpManagedServer>()

// --- Shared handler execution ---

async function runHandler(
  handler: { output?: JsonValue; script?: JsonValue },
  input: JsonValue,
  scriptDir: string,
  parentContext: ScriptContext,
): Promise<JsonValue | undefined> {
  if (handler.output !== undefined) {
    const vars = new Map<string, JsonValue>()
    vars.set('input', input)
    return resolveVariables(handler.output, vars)
  }

  if (handler.script !== undefined) {
    const ctx = new DefaultContext({
      scriptFile: scriptDir + '/handler.spec.yaml',
      workingDir: scriptDir,
      session: parentContext.session,
    })
    ctx.variables.set('input', input)
    setupSilentCapture(ctx)

    if (isString(handler.script)) {
      const filePath = resolve(scriptDir, handler.script)
      const content = readFileSync(filePath, 'utf-8')
      const script = Script.fromString(content)
      return script.run(ctx)
    } else {
      const script = Script.fromData(handler.script)
      return script.run(ctx)
    }
  }

  throw new SpecScriptError('No handler action defined — provide output or script')
}

function resultToString(result: JsonValue | undefined): string {
  if (result === undefined || result === null) return ''
  if (typeof result === 'string') return result
  return toDisplayYaml(result)
}

/**
 * Parse MCP text content back into a JsonValue. The server's resultToString
 * returns plain strings for scalar values and multi-line YAML for structured
 * data, so single-line text is returned as-is (avoids false positives where
 * the JS yaml parser interprets colons as map entries).
 */
function parseMcpTextContent(text: string): JsonValue {
  if (text.includes('\n')) {
    return parseYamlIfPossible(text)
  }
  return text
}

function writeToContext(context: ScriptContext | undefined, text: string): void {
  const writer = context?.session.get('stdout') as ((s: string) => void) | undefined
  if (writer) writer(text)
  else console.log(text)
}

// --- Tool registration ---

function addTool(
  managed: McpManagedServer,
  toolName: string,
  toolData: JsonObject,
  localContext: ScriptContext,
): void {
  writeToContext(localContext, ` - Tool: ${toolName}`)

  const inputSchemaRaw = toolData.inputSchema as JsonObject | undefined
  const needsDerivation = toolData.description === undefined || inputSchemaRaw === undefined
  const derived = needsDerivation ? deriveFromScript(toolData, localContext) : undefined
  const resolvedSchema = inputSchemaRaw
    ? toRawJsonSchema(inputSchemaRaw)
    : derived?.inputSchema
  const resolvedDescription = (toolData.description as string | undefined) ?? derived?.description ?? toolName

  managed.tools.push({
    name: toolName,
    description: resolvedDescription,
    inputSchema: resolvedSchema ?? { type: 'object' },
    handler: { output: toolData.output, script: toolData.script },
    context: localContext,
  })
}

function toRawJsonSchema(schema: JsonObject): RawJsonSchema {
  return {
    type: (schema.type as string) ?? 'object',
    properties: schema.properties as JsonObject | undefined,
    required: schema.required as string[] | undefined,
  }
}

interface DerivedToolMetadata {
  description?: string
  inputSchema?: RawJsonSchema
}

function deriveFromScript(
  tool: JsonObject,
  context: ScriptContext,
): DerivedToolMetadata | undefined {
  if (!isString(tool.script)) return undefined

  try {
    const filePath = resolve(context.scriptDir, tool.script)
    const content = readFileSync(filePath, 'utf-8')
    const commands = parseYamlCommands(content)

    // Derive description from Script info
    const scriptInfoCmd = commands.find(c => c.name.toLowerCase() === 'script info')
    const description = scriptInfoCmd && typeof scriptInfoCmd.data === 'string'
      ? scriptInfoCmd.data
      : undefined

    // Derive inputSchema from Input schema or Input parameters
    let inputSchema: RawJsonSchema | undefined

    const inputSchemaCmd = commands.find(c => c.name.toLowerCase() === 'input schema')
    if (inputSchemaCmd && isObject(inputSchemaCmd.data)) {
      inputSchema = toRawJsonSchema(inputSchemaCmd.data)
    } else {
      const inputParamsCmd = commands.find(c => c.name.toLowerCase() === 'input parameters')
      if (inputParamsCmd && isObject(inputParamsCmd.data)) {
        const properties: JsonObject = {}
        for (const [key, value] of Object.entries(inputParamsCmd.data)) {
          if (isObject(value) && value.type) {
            properties[key] = { type: value.type as string }
          } else if (isString(value)) {
            properties[key] = { type: value }
          } else {
            properties[key] = { type: 'string' }
          }
        }
        inputSchema = { type: 'object', properties }
      }
    }

    return { description, inputSchema }
  } catch {
    // File not found or parse error — no derivation
  }

  return undefined
}

// --- Resource registration ---

function addResource(
  managed: McpManagedServer,
  resourceURI: string,
  resourceData: JsonObject,
  localContext: ScriptContext,
): void {
  writeToContext(localContext, ` - Resource: ${resourceURI}`)

  managed.resources.push({
    uri: resourceURI,
    name: (resourceData.name as string) ?? resourceURI,
    description: (resourceData.description as string) ?? '',
    mimeType: (resourceData.mimeType as string) ?? 'text/plain',
    handler: { output: resourceData.output, script: resourceData.script },
    context: localContext,
  })
}

// --- Prompt registration ---

function addPrompt(
  managed: McpManagedServer,
  promptName: string,
  promptData: JsonObject,
  localContext: ScriptContext,
): void {
  writeToContext(localContext, ` - Prompt: ${promptName}`)

  const argsRaw = promptData.arguments as PromptArgumentInfo[] | undefined

  managed.prompts.push({
    name: promptName,
    description: (promptData.description as string) ?? '',
    arguments: argsRaw ?? [],
    handler: { output: promptData.output, script: promptData.script },
    context: localContext,
  })
}

// --- Protocol handler setup ---

function setupProtocolHandlers(managed: McpManagedServer, serverOverride?: McpLowLevelServer): void {
  const server = serverOverride ?? managed.server
  const { tools, resources, prompts } = managed

  server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: tools.map(t => ({
      name: t.name,
      description: t.description,
      inputSchema: t.inputSchema,
    })),
  }))

  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const tool = tools.find(t => t.name === request.params.name)
    if (!tool) {
      return {
        content: [{ type: 'text' as const, text: `Tool '${request.params.name}' not found` }],
        isError: true,
      }
    }

    try {
      const input = request.params.arguments ?? {}
      const result = await runHandler(tool.handler, input as JsonObject, tool.context.scriptDir, tool.context)
      return { content: [{ type: 'text' as const, text: resultToString(result) }] }
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e)
      return { content: [{ type: 'text' as const, text: msg }], isError: true }
    }
  })

  server.setRequestHandler(ListResourcesRequestSchema, async () => ({
    resources: resources.map(r => ({
      uri: r.uri,
      name: r.name,
      description: r.description,
      mimeType: r.mimeType,
    })),
  }))

  server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
    const resource = resources.find(r => r.uri === request.params.uri)
    if (!resource) {
      throw new SpecScriptError(`Resource '${request.params.uri}' not found`)
    }

    const result = await runHandler(resource.handler, {}, resource.context.scriptDir, resource.context)
    return {
      contents: [{
        uri: resource.uri,
        text: resultToString(result),
        mimeType: resource.mimeType,
      }],
    }
  })

  server.setRequestHandler(ListPromptsRequestSchema, async () => ({
    prompts: prompts.map(p => ({
      name: p.name,
      description: p.description,
      arguments: p.arguments.map(a => ({
        name: a.name,
        description: a.description,
        required: a.required,
      })),
    })),
  }))

  server.setRequestHandler(GetPromptRequestSchema, async (request) => {
    const prompt = prompts.find(p => p.name === request.params.name)
    if (!prompt) {
      throw new SpecScriptError(`Prompt '${request.params.name}' not found`)
    }

    const input = request.params.arguments ?? {}
    const result = await runHandler(prompt.handler, input as JsonObject, prompt.context.scriptDir, prompt.context)
    return {
      description: `Description for ${request.params.name}`,
      messages: [{
        role: 'user' as const,
        content: { type: 'text' as const, text: resultToString(result) },
      }],
    }
  })
}

// --- Server lifecycle ---

function createMcpServer(name: string, version: string): McpManagedServer {
  const server = new McpLowLevelServer(
    { name, version },
    {
      capabilities: {
        tools: { listChanged: true },
        resources: { listChanged: true },
        prompts: { listChanged: true },
      },
    },
  )

  const managed: McpManagedServer = {
    server,
    serverName: name,
    serverVersion: version,
    transportType: 'HTTP',
    tools: [],
    resources: [],
    prompts: [],
  }

  return managed
}

async function startServer(
  name: string,
  managed: McpManagedServer,
  transportType: TransportType,
  port: number,
  context: ScriptContext,
): Promise<void> {
  managed.transportType = transportType
  setupProtocolHandlers(managed)

  switch (transportType) {
    case 'STDIO':
      await startStdioServer(name, managed)
      break

    case 'HTTP':
      await startStreamableHttpServer(name, managed, port, context)
      break
  }
}

async function startStdioServer(
  name: string,
  managed: McpManagedServer,
): Promise<void> {
  const transport = new StdioServerTransport()
  await managed.server.connect(transport)
}

async function startStreamableHttpServer(
  name: string,
  managed: McpManagedServer,
  port: number,
  context: ScriptContext,
): Promise<void> {
  const httpServer = createServer(async (req: IncomingMessage, res: ServerResponse) => {
    const url = new URL(req.url || '/', `http://localhost:${port}`)

    if (url.pathname === '/mcp') {
      // Create a fresh server + transport per request (stateless mode).
      // The TS SDK requires a new Server.connect() per transport, and stateless mode
      // requires a new transport per request. This mirrors Kotlin's createSession().
      const perRequestServer = new McpLowLevelServer(
        { name: managed.serverName, version: managed.serverVersion },
        {
          capabilities: {
            tools: { listChanged: true },
            resources: { listChanged: true },
            prompts: { listChanged: true },
          },
        },
      )
      setupProtocolHandlers(managed, perRequestServer)
      const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: undefined })
      await perRequestServer.connect(transport)
      await transport.handleRequest(req, res)
    } else {
      res.writeHead(404)
      res.end('Not found')
    }
  })

  managed.httpServer = httpServer

  await new Promise<void>((resolve, reject) => {
    httpServer.on('error', reject)
    httpServer.listen(port, () => resolve())
  })

  writeToContext(context, `Started MCP HTTP server '${name}' on http://localhost:${port}/mcp`)
}

// --- Mcp server command ---

export const McpServerCommand: CommandHandler = {
  name: 'Mcp server',
  delayedResolver: true,
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Mcp server: expected an object')
    }

    const name = data.name as string
    if (!name) throw new CommandFormatError('Mcp server: missing required "name" property')
    const version = (data.version as string) ?? '1.0.0'
    const transport = ((data.transport as string) ?? 'HTTP').toUpperCase() as TransportType
    const port = (data.port as number) ?? 8080

    let managed = servers.get(name)
    const isNew = !managed

    if (!managed) {
      managed = createMcpServer(name, version)
      servers.set(name, managed)
    }

    // Register tools
    const tools = data.tools as JsonObject | undefined
    if (isObject(tools)) {
      for (const [toolName, toolData] of Object.entries(tools)) {
        if (isObject(toolData)) {
          addTool(managed, toolName, toolData, context.clone())
        }
      }
    }

    // Register resources
    const resources = data.resources as JsonObject | undefined
    if (isObject(resources)) {
      for (const [uri, resourceData] of Object.entries(resources)) {
        if (isObject(resourceData)) {
          addResource(managed, uri, resourceData, context.clone())
        }
      }
    }

    // Register prompts
    const prompts = data.prompts as JsonObject | undefined
    if (isObject(prompts)) {
      for (const [promptName, promptData] of Object.entries(prompts)) {
        if (isObject(promptData)) {
          addPrompt(managed, promptName, promptData, context.clone())
        }
      }
    }

    // Store current server name in session
    context.session.set(SESSION_KEY_DEFAULT_SERVER, name)

    // Start server if not already running
    if (isNew) {
      await startServer(name, managed, transport, port, context)
    }

    return undefined
  },
}

// --- Mcp tool command ---

export const McpToolCommand: CommandHandler = {
  name: 'Mcp tool',
  delayedResolver: true,
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Mcp tool: expected an object')
    }

    const managed = getDefaultServer(context)

    for (const [toolName, toolData] of Object.entries(data)) {
      if (isObject(toolData)) {
        addTool(managed, toolName, toolData, context.clone())
      }
    }

    return undefined
  },
}

// --- Mcp resource command ---

export const McpResourceCommand: CommandHandler = {
  name: 'Mcp resource',
  delayedResolver: true,
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Mcp resource: expected an object')
    }

    const managed = getDefaultServer(context)

    for (const [uri, resourceData] of Object.entries(data)) {
      if (isObject(resourceData)) {
        addResource(managed, uri, resourceData, context.clone())
      }
    }

    return undefined
  },
}

// --- Mcp prompt command ---

export const McpPromptCommand: CommandHandler = {
  name: 'Mcp prompt',
  delayedResolver: true,
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Mcp prompt: expected an object')
    }

    const managed = getDefaultServer(context)

    for (const [promptName, promptData] of Object.entries(data)) {
      if (isObject(promptData)) {
        addPrompt(managed, promptName, promptData, context.clone())
      }
    }

    return undefined
  },
}

// --- Mcp call tool command ---

export const McpCallToolCommand: CommandHandler = {
  name: 'Mcp call tool',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Mcp call tool: expected an object')
    }

    const toolName = data.tool as string
    if (!toolName) throw new CommandFormatError('Mcp call tool: missing required "tool" property')

    const serverInfo = data.server as JsonObject
    if (!isObject(serverInfo)) throw new CommandFormatError('Mcp call tool: missing required "server" property')

    const input = data.input as JsonObject | undefined
    const transport = createClientTransport(serverInfo)

    const client = new Client({ name: 'specscript-client', version: '1.0.0' })

    try {
      await client.connect(transport)

      const result = await client.callTool({
        name: toolName,
        arguments: input ?? {},
      })

      if (result.isError) {
        const errorText = result.content && Array.isArray(result.content) && result.content.length > 0
          ? (result.content[0] as { text?: string }).text ?? 'Unknown error'
          : 'Unknown error'
        throw new SpecScriptCommandError(
          `Tool '${toolName}' call failed`,
          { type: 'MCP Server error', data: errorText },
        )
      }

      // Extract first text content
      if (result.content && Array.isArray(result.content) && result.content.length > 0) {
        const first = result.content[0] as { type: string; text?: string }
        if (first.type === 'text' && first.text !== undefined) {
          return parseMcpTextContent(first.text)
        }
        return `Tool executed successfully with result of type ${first.type}`
      }

      return 'Tool executed but returned no content'
    } catch (e) {
      if (e instanceof SpecScriptCommandError) throw e
      const msg = e instanceof Error ? e.message : String(e)
      throw new SpecScriptCommandError(`Tool '${toolName}' call failed: ${msg}`)
    } finally {
      await client.close()
    }
  },
}

// --- Mcp read resource command ---

export const McpReadResourceCommand: CommandHandler = {
  name: 'Mcp read resource',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Mcp read resource: expected an object')
    }

    const uri = data.uri as string
    if (!uri) throw new CommandFormatError('Mcp read resource: missing required "uri" property')

    const serverInfo = data.server as JsonObject
    if (!isObject(serverInfo)) throw new CommandFormatError('Mcp read resource: missing required "server" property')

    const transport = createClientTransport(serverInfo)
    const client = new Client({ name: 'specscript-client', version: '1.0.0' })

    try {
      await client.connect(transport)

      const result = await client.readResource({ uri })

      if (result.contents && result.contents.length > 0) {
        const first = result.contents[0] as { text?: string }
        if (first.text !== undefined) {
          return parseMcpTextContent(first.text)
        }
      }

      return undefined
    } catch (e) {
      if (e instanceof SpecScriptCommandError) throw e
      const msg = e instanceof Error ? e.message : String(e)
      throw new SpecScriptCommandError(`Resource '${uri}' read failed: ${msg}`)
    } finally {
      await client.close()
    }
  },
}

// --- Mcp get prompt command ---

export const McpGetPromptCommand: CommandHandler = {
  name: 'Mcp get prompt',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Mcp get prompt: expected an object')
    }

    const promptName = data.prompt as string
    if (!promptName) throw new CommandFormatError('Mcp get prompt: missing required "prompt" property')

    const serverInfo = data.server as JsonObject
    if (!isObject(serverInfo)) throw new CommandFormatError('Mcp get prompt: missing required "server" property')

    const input = data.input as Record<string, string> | undefined
    const transport = createClientTransport(serverInfo)
    const client = new Client({ name: 'specscript-client', version: '1.0.0' })

    try {
      await client.connect(transport)

      const result = await client.getPrompt({
        name: promptName,
        arguments: input,
      })

      if (result.messages && result.messages.length > 0) {
        const first = result.messages[0]
        const content = first.content as { type: string; text?: string }
        if (content.type === 'text' && content.text !== undefined) {
          return parseMcpTextContent(content.text)
        }
      }

      return undefined
    } catch (e) {
      if (e instanceof SpecScriptCommandError) throw e
      const msg = e instanceof Error ? e.message : String(e)
      throw new SpecScriptCommandError(`Prompt '${promptName}' get failed: ${msg}`)
    } finally {
      await client.close()
    }
  },
}

function createClientTransport(serverInfo: JsonObject) {
  const command = serverInfo.command as string | undefined
  const url = serverInfo.url as string | undefined
  const transport = ((serverInfo.transport as string) ?? '').toUpperCase()
  const token = serverInfo.token as string | undefined
  const headers = serverInfo.headers as Record<string, string> | undefined

  if (command) {
    const [cmd, ...args] = command.split(/\s+/)
    return new StdioClientTransport({
      command: cmd,
      args,
      stderr: 'pipe',
    })
  }

  if (!url) {
    throw new SpecScriptError('Mcp call tool: server must specify either "url" or "command"')
  }

  const requestInit: RequestInit = {}
  const allHeaders: Record<string, string> = {}
  if (token) allHeaders['Authorization'] = `Bearer ${token}`
  if (headers) Object.assign(allHeaders, headers)
  if (Object.keys(allHeaders).length > 0) requestInit.headers = allHeaders

  // Default: Streamable HTTP
  return new StreamableHTTPClientTransport(new URL(url), {
    requestInit: Object.keys(requestInit).length > 0 ? requestInit : undefined,
  })
}

// --- Stop mcp server command ---

export const StopMcpServerCommand: CommandHandler = {
  name: 'Stop mcp server',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (typeof data !== 'string') {
      throw new CommandFormatError('Stop mcp server: expected a server name string')
    }
    await stopServer(data, context)
    return undefined
  },
}

// --- Server lifecycle helpers ---

function getDefaultServer(context: ScriptContext): McpManagedServer {
  const name = context.session.get(SESSION_KEY_DEFAULT_SERVER) as string | undefined
  if (!name) {
    throw new SpecScriptError(
      'No MCP server found in current context. An MCP server must be started before defining tools.',
    )
  }
  const managed = servers.get(name)
  if (!managed) {
    throw new SpecScriptError(
      `MCP server '${name}' is not running. Start it with Mcp server before defining tools.`,
    )
  }
  return managed
}

async function stopServer(name: string, context?: ScriptContext): Promise<void> {
  const managed = servers.get(name)
  if (!managed) return

  servers.delete(name)

  try {
    await managed.server.close()
  } catch {
    // Server may already be closed
  }

  if (managed.httpServer) {
    await new Promise<void>((resolve) => {
      managed.httpServer!.close(() => resolve())
      setTimeout(() => resolve(), 200)
    })
  }

  if (context) {
    context.session.delete(SESSION_KEY_DEFAULT_SERVER)
  }
}

export async function stopAllMcpServers(): Promise<void> {
  const names = Array.from(servers.keys())
  for (const name of names) {
    await stopServer(name)
  }
}
