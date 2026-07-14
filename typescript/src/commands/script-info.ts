import type {CommandHandler} from '../language/command-handler.js'
import type {JsonObject, JsonValue} from '../language/types.js'
import {isObject, MissingInputError} from '../language/types.js'
import type {ScriptContext} from '../language/context.js'
import {resolveValue, passesCondition} from './prompt.js'

/**
 * Script info: declares metadata about a script. No-op during execution.
 */
export const ScriptInfo: CommandHandler = {
    name: 'Script info',
    delayedResolver: true,

    async execute(_data: JsonValue, _context: ScriptContext): Promise<JsonValue | undefined> {
        return undefined
    },
}

/**
 * Input parameters: declares named input parameters with defaults, conditions, etc.
 * Each key in the data object is a parameter name.
 * The value is a parameter definition with optional: default, description, condition, env.
 */
export const InputParameters: CommandHandler = {
    name: 'Input parameters',
    delayedResolver: true,

    async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
        if (!isObject(data)) return getInput(context)

        await populateInputVariables(context, data)
        return getInput(context)
    },
}

/**
 * Input schema: declares a JSON Schema for script input.
 * Extracts properties from the schema and delegates to the same population logic.
 */
export const InputSchema: CommandHandler = {
    name: 'Input schema',
    delayedResolver: true,

    async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
        if (!isObject(data)) return getInput(context)

        // Extract properties from JSON Schema format
        const properties = data['properties']
        if (!isObject(properties)) return getInput(context)

        await populateInputVariables(context, properties)
        return getInput(context)
    },
}

/**
 * Get the input variable as an object, ensuring it exists.
 */
function getInput(context: ScriptContext): JsonObject {
    const input = context.variables.get('input') as JsonValue
    if (isObject(input)) return input
    const obj: JsonObject = {}
    context.variables.set('input', obj)
    return obj
}

/**
 * Populate input variables from parameter definitions.
 *
 * Uses the shared per-property resolver (see resolveValue): already-set input → x-env → recorded
 * answer → interactive prompt (default as hint) → default → error.
 */
async function populateInputVariables(context: ScriptContext, parameters: JsonObject): Promise<void> {
    const input = getInput(context)

    for (const [name, paramDef] of Object.entries(parameters)) {
        const def = isObject(paramDef) ? paramDef : {description: paramDef}

        // Already provided as input — just copy to top-level
        if (name in input) {
            context.variables.set(name, input[name])
            continue
        }

        // Skip if condition is not valid
        if (!passesCondition(def, context.variables)) {
            continue
        }

        // Resolve from environment variable, recorded answer, interactive prompt or default
        const value = await resolveValue(context, def, name, true)
        if (value === undefined) {
            throw new MissingInputError(`No value provided for: ${name}`, name)
        }

        setInputValue(context, input, name, value)
    }
}

/**
 * Set a value in both the input object and as a top-level variable.
 */
function setInputValue(context: ScriptContext, input: JsonObject, name: string, value: JsonValue): void {
    input[name] = value
    context.variables.set(name, value)
    // Also update the input variable reference in context
    context.variables.set('input', input)
}
