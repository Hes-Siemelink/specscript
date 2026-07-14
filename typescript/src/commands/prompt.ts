/**
 * Prompt command: asks the user for interactive input, described as a JSON Schema.
 *
 * Forms:
 * - String: `Prompt: What is your name?` — shorthand for a single text question
 * - Object schema with `properties` — asks several questions, returns an object
 * - Any other object — a single property definition
 *
 * The answer is returned as `${output}`. Prompt and Input schema share the same per-property
 * resolution (see resolveValue).
 */

import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isString, SpecScriptError } from '../language/types.js'
import { toCondition } from '../language/conditions.js'
import { resolveVariables } from '../language/variables.js'
import {
  promptText,
  promptSelect,
  getAnswers,
  NON_INTERACTIVE_PLACEHOLDER,
} from '../language/user-prompt.js'
import type { Choice } from '../language/user-prompt.js'
import { toDisplayYaml } from '../util/yaml.js'

export const PromptCommand: CommandHandler = {
  name: 'Prompt',
  delayedResolver: true,

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isString(data)) {
      const text = String(resolveVariables(data, context.variables))
      return resolveOrPlaceholder(context, { description: text })
    }

    if (isObject(data)) {
      // Object schema: ask each property, collect answers into an object
      if (isObject(data['properties'])) {
        return promptProperties(context, data['properties'] as JsonObject)
      }

      // Single property
      const resolved = resolveVariables(data, context.variables)
      if (!isObject(resolved)) return undefined
      if (!passesCondition(resolved, context.variables)) return undefined
      return resolveOrPlaceholder(context, resolved)
    }

    return undefined
  },
}

/** Asks each property of an object schema, allowing later questions to depend on earlier answers. */
async function promptProperties(context: ScriptContext, properties: JsonObject): Promise<JsonObject> {
  const result: JsonObject = {}
  const variables = new Map(context.variables)

  for (const [name, propertyNode] of Object.entries(properties)) {
    const resolved = resolveVariables(propertyNode, variables)
    const def = isString(resolved) ? { description: resolved } : isObject(resolved) ? resolved : undefined
    if (def === undefined) continue
    if (!passesCondition(def, variables)) continue

    const answer = await resolveOrPlaceholder(context, def, name)
    result[name] = answer
    variables.set(name, answer)
  }

  return result
}

/** Resolves a value; a missing text prompt yields the placeholder, a missing choice is an error. */
async function resolveOrPlaceholder(context: ScriptContext, def: JsonObject, name?: string): Promise<JsonValue> {
  const value = await resolveValue(context, def, name, false)
  if (value !== undefined) return value

  if (isChoice(def)) {
    throw new SpecScriptError(`No value selected for '${question(def, name)}' and not in interactive mode`)
  }
  return NON_INTERACTIVE_PLACEHOLDER
}

// ---------------------------------------------------------------------------
// Shared per-property resolution (used by Prompt and Input schema)
// ---------------------------------------------------------------------------

/**
 * Resolves a single property to a value, or undefined if nothing resolves.
 *
 * Order: environment variable (when checkEnv) → recorded answer → interactive prompt (with the
 * default as a hint) → default value.
 */
export async function resolveValue(
  context: ScriptContext,
  def: JsonObject,
  name: string | undefined,
  checkEnv: boolean,
): Promise<JsonValue | undefined> {

  // Environment variable (input sources only)
  if (checkEnv && isString(def['x-env'])) {
    const envValue = process.env[def['x-env']]
    if (envValue !== undefined) return envValue
  }

  // Recorded answer or interactive prompt
  const asked = await ask(context, def, question(def, name))
  if (asked !== undefined) return asked

  // Fallback to default value (raw type preserved)
  return def['default']
}

/** The question shown to the user: title → description → name. */
export function question(def: JsonObject, name?: string): string {
  if (isString(def['title'])) return def['title']
  if (isString(def['description'])) return def['description']
  return name ?? ''
}

/** Whether a value must be selected rather than freely typed. */
export function isChoice(def: JsonObject): boolean {
  return Array.isArray(def['enum']) || def['type'] === 'array'
}

/** Evaluates an `x-condition`, resolving its variables first. Absent condition passes. */
export function passesCondition(def: JsonObject, variables: Map<string, JsonValue>): boolean {
  const condition = def['x-condition']
  if (condition === undefined || condition === null) return true
  const resolved = resolveVariables(condition, variables)
  if (isObject(resolved)) return toCondition(resolved).isTrue()
  return true
}

/** Asks by type: array → multi-select, enum → single-select, password, boolean, or text. */
async function ask(context: ScriptContext, def: JsonObject, message: string): Promise<JsonValue | undefined> {
  const answers = getAnswers(context.session)
  const stdout = context.session.get('stdout') as ((text: string) => void) | undefined
  const type = def['type']
  const defaultHint = def['default'] !== undefined ? String(def['default']) : ''

  if (type === 'array') {
    const items = isObject(def['items']) ? (def['items'] as JsonObject) : undefined
    if (items === undefined) return undefined
    const itemsEnum = Array.isArray(items['enum']) ? (items['enum'] as JsonValue[]) : []
    return promptChoice(answers, message, itemsEnum, asString(items['x-title-property']), asString(items['x-value-property']), true, stdout, context.interactive)
  }

  if (Array.isArray(def['enum'])) {
    return promptChoice(answers, message, def['enum'] as JsonValue[], asString(def['x-title-property']), asString(def['x-value-property']), false, stdout, context.interactive)
  }

  if (def['format'] === 'password') {
    return promptText(answers, message, defaultHint, true, stdout, context.interactive)
  }

  if (type === 'boolean') {
    const answer = await promptText(answers, message, defaultHint, false, stdout, context.interactive)
    if (answer === undefined) return undefined
    return answer === true || answer === 'true'
  }

  return promptText(answers, message, defaultHint, false, stdout, context.interactive)
}

async function promptChoice(
  answers: Map<string, JsonValue>,
  message: string,
  enumValues: JsonValue[],
  titleProperty: string | undefined,
  valueProperty: string | undefined,
  multiple: boolean,
  stdout?: (text: string) => void,
  interactive: boolean = false,
): Promise<JsonValue | undefined> {
  const choices: Choice[] = enumValues.map(choiceData => {
    if (titleProperty && isObject(choiceData)) {
      return { displayName: String((choiceData as JsonObject)[titleProperty]), value: choiceData }
    }
    return { displayName: toDisplayYaml(choiceData), value: choiceData }
  })

  const answer = await promptSelect(answers, message, choices, multiple, stdout, interactive)
  if (answer === undefined) return undefined

  return onlyWith(answer, valueProperty)
}

/** Extract a specific field from the selected value(s). */
function onlyWith(value: JsonValue, field: string | undefined): JsonValue {
  if (!field) return value

  if (Array.isArray(value)) {
    return value.map(item => (isObject(item) ? (item as JsonObject)[field] : item))
  }

  if (isObject(value)) {
    return (value as JsonObject)[field]
  }

  return value
}

function asString(value: JsonValue | undefined): string | undefined {
  return typeof value === 'string' ? value : undefined
}
