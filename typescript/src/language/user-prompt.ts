/**
 * User prompt abstraction for interactive input.
 *
 * Two modes of operation:
 * - TestPrompt: reads pre-recorded answers from context.session, prints simulated TUI output
 * - InquirerPrompt: uses @inquirer/prompts for real terminal interaction
 *
 * Dispatch is per-call: if a recorded answer exists for the question, TestPrompt behavior
 * is used. Otherwise, falls through to @inquirer/prompts.
 */

import { input, password, select, checkbox } from '@inquirer/prompts'
import type { JsonValue, JsonObject } from './types.js'
import { isObject } from './types.js'
import { toDisplayYaml } from '../util/yaml.js'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface Choice {
  displayName: string
  value: JsonValue
}

type AnswersMap = Map<string, JsonValue>

// ---------------------------------------------------------------------------
// Prompt functions (dispatch between test and real)
// ---------------------------------------------------------------------------

/**
 * Prompt for text input.
 * If a recorded answer exists for the message, returns it with its original type (TestPrompt behavior).
 * Otherwise, uses @inquirer/prompts input() which always returns a string.
 */
export async function promptText(
  answers: AnswersMap,
  message: string,
  defaultValue: string = '',
  isPassword: boolean = false,
  stdout?: (text: string) => void,
): Promise<JsonValue> {
  const recorded = answers.get(message)
  if (recorded !== undefined) {
    const displayValue = typeof recorded === 'string' ? recorded : toDisplayYaml(recorded)
    if (isPassword) {
      writeOutput(stdout, renderTextPrompt(message, '********'))
    } else {
      writeOutput(stdout, renderTextPrompt(message, displayValue))
    }
    // Return the raw recorded value, preserving its original type (matches Kotlin's TestPrompt)
    return recorded
  }

  // Fall back to default if available
  if (defaultValue) {
    writeOutput(stdout, renderTextPrompt(message, defaultValue))
    return defaultValue
  }

  // Real interactive prompt
  if (isPassword) {
    return await password({ message })
  }
  return await input({ message, default: defaultValue || undefined })
}

/**
 * Prompt for selection from a list of choices.
 * If a recorded answer exists, uses it (TestPrompt behavior).
 * Otherwise, uses @inquirer/prompts select() or checkbox().
 */
export async function promptSelect(
  answers: AnswersMap,
  message: string,
  choices: Choice[],
  multiple: boolean = false,
  stdout?: (text: string) => void,
): Promise<JsonValue> {
  const recorded = answers.get(message)

  if (recorded !== undefined) {
    if (multiple) {
      // Answer is an array of display names
      const selectedNames = Array.isArray(recorded)
        ? recorded.map(v => typeof v === 'string' ? v : toDisplayYaml(v))
        : [typeof recorded === 'string' ? recorded : toDisplayYaml(recorded)]
      const selection = choices.filter(c => selectedNames.includes(c.displayName))
      writeOutput(stdout, renderSelectPrompt(message, choices, selection))
      return selection.map(c => c.value)
    } else {
      // Answer is a display name string
      const answerStr = typeof recorded === 'string' ? recorded : toDisplayYaml(recorded)
      const selection = choices.find(c => c.displayName === answerStr)
      if (!selection) {
        throw new Error(`Prerecorded choice '${answerStr}' not found in provided list.`)
      }
      writeOutput(stdout, renderSelectPrompt(message, choices, [selection]))
      return selection.value
    }
  }

  // Real interactive prompt
  if (multiple) {
    const result = await checkbox({
      message,
      choices: choices.map(c => ({ name: c.displayName, value: c.value })),
    })
    return result
  } else {
    const result = await select({
      message,
      choices: choices.map(c => ({ name: c.displayName, value: c.value })),
    })
    return result
  }
}

// ---------------------------------------------------------------------------
// TestPrompt output rendering (must match Kotlin's TestPrompt exactly)
// ---------------------------------------------------------------------------

/**
 * Render text prompt output: `? message answer`
 */
function renderTextPrompt(message: string, answer: string): string {
  let result = `? ${message} `
  if (answer) {
    result += answer
  }
  return result
}

/**
 * Render select prompt output:
 * ```
 * ? message
 *  ❯ ◉ selected
 *    ◯ other
 * ```
 */
function renderSelectPrompt(message: string, choices: Choice[], selected: Choice[]): string {
  let result = `? ${message} \n`
  let first = true
  for (const choice of choices) {
    const isSelected = selected.some(s => s.displayName === choice.displayName)
    if (isSelected) {
      if (first) {
        result += ` ❯ ◉ `
        first = false
      } else {
        result += `   ◉ `
      }
    } else {
      result += `   ◯ `
    }
    result += choice.displayName + '\n'
  }
  return result
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function writeOutput(stdout: ((text: string) => void) | undefined, text: string): void {
  if (stdout) {
    stdout(text)
  }
}

/**
 * Get the answers map from a script context session.
 */
export function getAnswers(session: Map<string, unknown>): AnswersMap {
  return (session.get('answers') as AnswersMap) ?? new Map<string, JsonValue>()
}
