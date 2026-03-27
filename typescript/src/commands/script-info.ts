import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isString, SpecScriptError, MissingInputError } from '../language/types.js'
import type { ScriptContext } from '../language/context.js'
import { resolveVariables } from '../language/variables.js'
import { toCondition } from '../language/conditions.js'
import { getAnswers } from '../language/user-prompt.js'
import { doPrompt } from './prompt.js'

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
 * Resolution order (matches Kotlin's InputParameters):
 * 1. Already exists in input → copy to top-level variable
 * 2. Check condition → skip if false
 * 3. Environment variable → use env value
 * 4. Default value → use default
 * 5. Recorded answers → use answer from Answers command
 * 6. Interactive mode → prompt user via doPrompt()
 * 7. Error → throw MissingInputError
 */
async function populateInputVariables(context: ScriptContext, parameters: JsonObject): Promise<void> {
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
    let value: JsonValue | undefined

    // Check environment variable
    if ('env' in def && typeof def['env'] === 'string') {
      const envValue = process.env[def['env']]
      if (envValue !== undefined) {
        setInputValue(context, input, name, envValue)
        continue
      }
    }

    // Use default
    if ('default' in def) {
      setInputValue(context, input, name, def['default'])
      continue
    }

    // Check recorded answers (matches Kotlin: Answers.hasRecordedAnswer before interactive)
    const question = (isString(def['description']) ? def['description'] : undefined) ?? name
    const answers = getAnswers(context.session)
    if (answers.has(question)) {
      setInputValue(context, input, name, answers.get(question)!)
      continue
    }

    // Interactive mode — prompt user
    if (context.interactive) {
      const result = await doPrompt(context, def as JsonObject, name)
      if (result !== null && result !== undefined) {
        setInputValue(context, input, name, result)
      }
      continue
    }

    // No value available — throw
    throw new MissingInputError(`No value provided for: ${name}`, name)
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
