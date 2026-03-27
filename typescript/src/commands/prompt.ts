/**
 * Prompt command: asks the user for interactive input.
 *
 * Supports two forms:
 * - Value: `Prompt: What is your name?` — simple text prompt
 * - Object: `Prompt: {description, default, secret, enum, select, display property, value property, condition, type}`
 */

import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isString } from '../language/types.js'
import { toCondition } from '../language/conditions.js'
import { promptText, promptSelect, getAnswers } from '../language/user-prompt.js'
import type { Choice } from '../language/user-prompt.js'
import { toDisplayYaml } from '../util/yaml.js'

export const PromptCommand: CommandHandler = {
  name: 'Prompt',

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isString(data)) {
      const result = await doPrompt(context, { description: data })
      return result ?? undefined
    }

    if (isObject(data)) {
      const result = await doPrompt(context, data)
      return result ?? undefined
    }

    return undefined
  },
}

/**
 * Core prompt logic — resolves prompt definition and dispatches to the appropriate prompt type.
 * Matches Kotlin's ParameterDataPrompt resolution order.
 */
export async function doPrompt(
  context: ScriptContext,
  def: JsonObject,
  label?: string,
): Promise<JsonValue | null> {

  const message = (isString(def['description']) ? def['description'] : label) ?? ''
  const defaultValue = def['default'] !== undefined ? String(def['default']) : ''
  const secret = def['secret'] === true
  const enumValues = Array.isArray(def['enum']) ? def['enum'] as JsonValue[] : undefined
  const selectMode = isString(def['select']) ? def['select'] : 'single'
  const displayProperty = isString(def['display property']) ? def['display property'] : undefined
  const valueProperty = isString(def['value property']) ? def['value property'] : undefined
  const type = def['type']

  // Check condition
  if (def['condition'] !== undefined && def['condition'] !== null) {
    const condition = toCondition(def['condition'])
    if (!condition.isTrue()) {
      return null
    }
  }

  const answers = getAnswers(context.session)
  const stdout = context.session.get('stdout') as ((text: string) => void) | undefined

  // Resolution order (matches Kotlin's ParameterDataPrompt):
  // 1. enum + single select → single-choice
  // 2. enum + multiple select → multi-choice
  // 3. secret → password prompt
  // 4. type with properties → recursive property prompting
  // 5. type = boolean → boolean prompt
  // 6. type = string → text prompt
  // 7. default → text prompt

  if (enumValues !== undefined && selectMode === 'single') {
    return promptChoice(answers, message, enumValues, displayProperty, valueProperty, false, stdout)
  }

  if (enumValues !== undefined && selectMode === 'multiple') {
    return promptChoice(answers, message, enumValues, displayProperty, valueProperty, true, stdout)
  }

  if (secret) {
    const result = await promptText(answers, message, defaultValue, true, stdout)
    return result
  }

  if (type !== undefined && type !== null) {
    return promptByType(context, answers, message, type, defaultValue, stdout)
  }

  const result = await promptText(answers, message, defaultValue, false, stdout)
  return result
}

// ---------------------------------------------------------------------------
// Choice prompting
// ---------------------------------------------------------------------------

async function promptChoice(
  answers: Map<string, JsonValue>,
  message: string,
  enumValues: JsonValue[],
  displayProperty: string | undefined,
  valueProperty: string | undefined,
  multiple: boolean,
  stdout?: (text: string) => void,
): Promise<JsonValue> {
  const choices: Choice[] = enumValues.map(choiceData => {
    if (displayProperty && isObject(choiceData)) {
      return { displayName: String((choiceData as JsonObject)[displayProperty]), value: choiceData }
    }
    return { displayName: toDisplayYaml(choiceData), value: choiceData }
  })

  const answer = await promptSelect(answers, message, choices, multiple, stdout)

  return onlyWith(answer, valueProperty)
}

/**
 * Extract a specific field from the selected value(s), matching Kotlin's onlyWith().
 */
function onlyWith(value: JsonValue, field: string | undefined): JsonValue {
  if (!field) return value

  if (Array.isArray(value)) {
    return value.map(item => isObject(item) ? (item as JsonObject)[field] : item)
  }

  if (isObject(value)) {
    return (value as JsonObject)[field]
  }

  return value
}

// ---------------------------------------------------------------------------
// Type-based prompting
// ---------------------------------------------------------------------------

async function promptByType(
  context: ScriptContext,
  answers: Map<string, JsonValue>,
  message: string,
  type: JsonValue,
  defaultValue: string,
  stdout?: (text: string) => void,
): Promise<JsonValue> {
  // String type name
  if (isString(type)) {
    switch (type) {
      case 'boolean': return promptBoolean(answers, message, defaultValue, stdout)
      case 'string': return promptText(answers, message, defaultValue, false, stdout)
      default: return promptText(answers, message, defaultValue, false, stdout)
    }
  }

  if (isObject(type)) {
    const typeObj = type as JsonObject

    // Inline properties → recursive object prompting
    if (isObject(typeObj['properties'])) {
      return promptObjectProperties(context, typeObj['properties'] as JsonObject)
    }

    // Base type
    if (isString(typeObj['base'])) {
      switch (typeObj['base']) {
        case 'boolean': return promptBoolean(answers, message, defaultValue, stdout)
        case 'string': return promptText(answers, message, defaultValue, false, stdout)
      }
    }
  }

  return promptText(answers, message, defaultValue, false, stdout)
}

async function promptBoolean(
  answers: Map<string, JsonValue>,
  message: string,
  defaultValue: string,
  stdout?: (text: string) => void,
): Promise<boolean> {
  const answer = await promptText(answers, message, defaultValue, false, stdout)
  return answer === true || answer === 'true'
}

/**
 * Prompt for each property in a type definition (recursive prompting).
 */
async function promptObjectProperties(
  context: ScriptContext,
  properties: JsonObject,
): Promise<JsonObject> {
  const result: JsonObject = {}
  const answers = getAnswers(context.session)
  const stdout = context.session.get('stdout') as ((text: string) => void) | undefined

  for (const [name, propDef] of Object.entries(properties)) {
    let def: JsonObject
    if (isString(propDef)) {
      def = { description: propDef }
    } else if (isObject(propDef)) {
      def = propDef as JsonObject
    } else {
      continue
    }

    // Check condition
    if (def['condition'] !== undefined && def['condition'] !== null) {
      const condition = toCondition(def['condition'])
      if (!condition.isTrue()) continue
    }

    const propMessage = (isString(def['description']) ? def['description'] : name) ?? name
    const propDefault = def['default'] !== undefined ? String(def['default']) : ''

    const answer = await promptText(answers, propMessage, propDefault, def['secret'] === true, stdout)
    result[name] = answer
  }

  return result
}
