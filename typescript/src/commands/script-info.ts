import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, SpecScriptError, MissingInputError } from '../language/types.js'
import type { ScriptContext } from '../language/context.js'
import { resolveVariables } from '../language/variables.js'
import { toCondition } from '../language/conditions.js'

/**
 * Script info: declares metadata about a script. No-op during execution.
 */
export const ScriptInfo: CommandHandler = {
  name: 'Script info',
  delayedResolver: true,

  execute(_data: JsonValue, _context: ScriptContext): JsonValue | undefined {
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

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    if (!isObject(data)) return getInput(context)

    populateInputVariables(context, data)
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

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    if (!isObject(data)) return getInput(context)

    // Extract properties from JSON Schema format
    const properties = data['properties']
    if (!isObject(properties)) return getInput(context)

    populateInputVariables(context, properties)
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
 * For each parameter:
 * 1. If it already exists in input, copy to top-level variable
 * 2. Check condition (resolved against current variables) — skip if false
 * 3. Use default value, env variable, or throw if missing
 * 4. Set in input object AND as top-level variable
 */
function populateInputVariables(context: ScriptContext, parameters: JsonObject): void {
  const input = getInput(context)

  for (const [name, paramDef] of Object.entries(parameters)) {
    const def = isObject(paramDef) ? paramDef : { description: paramDef }

    // Already exists in input — just copy to top-level
    if (name in input) {
      context.variables.set(name, input[name])
      continue
    }

    // Check condition
    if ('condition' in def && def['condition'] !== null) {
      const resolvedCondition = resolveVariables(def['condition'], context.variables)
      if (isObject(resolvedCondition)) {
        const condition = toCondition(resolvedCondition)
        if (!condition.isTrue()) {
          continue
        }
      }
    }

    // Find value
    let value: JsonValue

    // Check environment variable
    if ('env' in def && typeof def['env'] === 'string') {
      const envValue = process.env[def['env']]
      if (envValue !== undefined) {
        value = envValue
        setInputValue(context, input, name, value)
        continue
      }
    }

    // Use default
    if ('default' in def) {
      value = def['default']
      setInputValue(context, input, name, value)
      continue
    }

    // No value available — in non-interactive mode, throw
    if (!context.interactive) {
      throw new MissingInputError(`No value provided for: ${name}`, name)
    }
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
