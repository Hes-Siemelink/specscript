/**
 * Run command: execute another .spec.yaml/.spec.md file or inline script.
 */

import {existsSync, readFileSync} from 'node:fs'
import {resolve as resolvePath} from 'node:path'
import type {CommandHandler} from '../language/command-handler.js'
import type {ScriptContext} from '../language/context.js'
import {DefaultContext, setRunFileFn} from '../language/context.js'
import type {JsonValue} from '../language/types.js'
import {CommandFormatError, isArray, isObject, isScalar, isString} from '../language/types.js'
import {Script} from '../language/script.js'
import {parseMarkdownScripts} from '../markdown/converter.js'
import {propagateConnectionOverrides} from './connect-to.js'
import {resolve as resolveData} from '../language/command-execution.js'
import {getDefaultProperty} from './script-info.js'

export const RunCommand: CommandHandler = {
    name: 'Run',
    delayedResolver: true,
    async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
        if (isString(data)) {
            // Value form: resolve the string, then run relative to scriptDir
            const resolved = await resolveData(data, context) as string
            const filePath = resolvePath(context.scriptDir, resolved)
            return runScriptFile(filePath, {}, context)
        }

        if (isObject(data)) {
            // Selectively resolve metadata fields — NOT the inline script body
            const cd = data.cd ? await resolveData(data.cd, context) as string : undefined
            const cdPath = cd ? resolvePath(cd) : undefined
            const scriptNode = data.script
            const fileNode = data.file

            if (fileNode !== undefined) {
                // file: resolves relative to workingDir (or cd if set)
                const baseDir = cdPath ?? context.workingDir
                const resolvedFile = await resolveData(fileNode, context) as string
                const filePath = resolvePath(baseDir, resolvedFile)
                const input = data.input ? await resolveData(data.input, context) : {}
                return runWithInput(filePath, input, context)
            }

            if (scriptNode !== undefined && isString(scriptNode)) {
                // script: string — resolves relative to scriptDir (or cd if set)
                const baseDir = cdPath ?? context.scriptDir
                const resolvedScript = await resolveData(scriptNode, context) as string
                const filePath = resolvePath(baseDir, resolvedScript)
                const input = data.input ? await resolveData(data.input, context) : {}
                return runWithInput(filePath, input, context)
            }

            if (scriptNode !== undefined) {
                // script: inline — do NOT resolve the script body; it runs in its own context
                return runInlineScript(scriptNode, cdPath, context)
            }

            throw new CommandFormatError("Run: expected 'script' or 'file' property")
        }

        throw new CommandFormatError('Run: expected filename string or object')
    },
}

/**
 * Run a script file with input, handling auto-list iteration.
 */
async function runWithInput(filePath: string, input: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isArray(input)) {
        const results: (JsonValue | undefined)[] = []
        let hasResults = false
        for (const item of input) {
            const result = await runScriptFile(filePath, item, context)
            if (result !== undefined) {
                results.push(result)
                hasResults = true
            } else {
                results.push(null)
            }
        }
        if (!hasResults) return undefined
        const output = results as JsonValue
        context.output = output
        return output
    }
    return runScriptFile(filePath, input, context)
}

/**
 * Run a script file with the given input, creating a child context.
 * Exported for use by local file command resolution.
 */
export async function runScriptFile(filePath: string, input: JsonValue, parentContext: ScriptContext): Promise<JsonValue | undefined> {
    if (!existsSync(filePath)) {
        throw new CommandFormatError(`File not found: ${filePath}`)
    }

    const content = readFileSync(filePath, 'utf-8')
    const isMarkdown = filePath.endsWith('.spec.md')
    const scripts = isMarkdown ? parseMarkdownScripts(content) : [Script.fromString(content)]

    // Scalar input fills the script's default property (declared via x-default-property)
    const scriptInput = isScalar(input) ? wrapDefaultProperty(scripts, filePath, input) : input

    // Create a child context with fresh variables but shared session
    const childContext = (parentContext as DefaultContext).createChildContext(filePath, scriptInput)

    // Propagate parent's connection definitions for inheritance (first one wins)
    propagateConnectionOverrides(parentContext.scriptDir, childContext)

    if (isMarkdown) {
        for (const script of scripts) {
            await script.run(childContext)
        }
        return childContext.output
    } else {
        return scripts[0].run(childContext)
    }
}

/**
 * Puts scalar input into the script's default property. Throws if the script declares none.
 */
function wrapDefaultProperty(scripts: Script[], filePath: string, value: JsonValue): JsonValue {
    for (const script of scripts) {
        const inputSchema = script.commands.find(c => c.name.toLowerCase() === 'input schema')
        if (inputSchema) {
            const property = getDefaultProperty(inputSchema.data)
            if (property) return {[property]: value}
        }
    }
    throw new CommandFormatError(`Script '${filePath}' does not accept a value; declare x-default-property in Input schema`)
}

/**
 * Run an inline script in a child context with variable isolation.
 */
async function runInlineScript(scriptNode: JsonValue, cdPath: string | undefined, context: ScriptContext): Promise<JsonValue | undefined> {
    const childContext = (context as DefaultContext).createInlineChildContext(cdPath)
    const script = Script.fromData(scriptNode)
    return script.run(childContext)
}

// Register the runScriptFile function to break circular dependency with context.ts
setRunFileFn(runScriptFile)
