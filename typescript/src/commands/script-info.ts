import type {CommandHandler} from '../language/command-handler.js'
import type {JsonObject, JsonValue} from '../language/types.js'
import {CommandFormatError, isObject, isString, MissingInputError} from '../language/types.js'
import type {ScriptContext} from '../language/context.js'
import {passesCondition, resolveValue} from './prompt.js'

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
 * Input schema: declares a JSON Schema for script input.
 * Extracts properties from the schema and delegates to the same population logic.
 */
export const InputSchema: CommandHandler = {
    name: 'Input schema',
    delayedResolver: true,

    async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
        if (!isObject(data)) return getInput(context)

        // Validate x-default-property whenever the schema is processed (load-time check)
        getDefaultProperty(data)

        // Extract properties from JSON Schema format
        const properties = data['properties']
        if (!isObject(properties)) return getInput(context)

        await populateInputVariables(context, properties)
        return getInput(context)
    },
}

/**
 * Read and validate the x-default-property from an Input schema command's data.
 * Returns the named property, or undefined when none is declared. Throws (load-time) if the
 * annotation names a missing or non-scalar property.
 */
export function getDefaultProperty(inputSchemaData: JsonValue): string | undefined {
    if (!isObject(inputSchemaData)) return undefined

    const defaultProperty = inputSchemaData['x-default-property']
    if (defaultProperty === undefined) return undefined

    if (!isString(defaultProperty)) {
        throw new CommandFormatError('x-default-property must be a string')
    }

    const properties = inputSchemaData['properties']
    const property = isObject(properties) ? properties[defaultProperty] : undefined
    if (property === undefined) {
        throw new CommandFormatError(`x-default-property '${defaultProperty}' is not defined in properties`)
    }
    if (!isScalarProperty(property)) {
        throw new CommandFormatError(`x-default-property '${defaultProperty}' must be a scalar property`)
    }

    return defaultProperty
}

/**
 * A scalar property holds a single value (string, number, integer, boolean), not an object or array.
 */
function isScalarProperty(property: JsonValue): boolean {
    if (!isObject(property)) return true
    const type = property['type']
    if (!isString(type)) return true
    return type !== 'object' && type !== 'array'
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
