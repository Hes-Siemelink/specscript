/**
 * User prompt abstraction for interactive input.
 *
 * Two modes of operation:
 * - Recorded answers: if a pre-recorded answer exists for the message, use it and print simulated output
 * - Default value: if no recorded answer but a default exists, use the default
 * - Interactive: if no recorded answer and no default, prompt the user (only when interactive=true)
 * - Non-interactive with no answer/default: return empty string
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
 * Resolution: recorded answer → default value → interactive prompt (if allowed) → empty string.
 */
export async function promptText(
  answers: AnswersMap,
  message: string,
  defaultValue: string = '',
  isPassword: boolean = false,
  stdout?: (text: string) => void,
  interactive: boolean = false,
): Promise<JsonValue> {
  const recorded = answers.get(message)
  if (recorded !== undefined) {
    const displayValue = typeof recorded === 'string' ? recorded : toDisplayYaml(recorded)
    if (isPassword) {
      writeOutput(stdout, renderTextPrompt(message, '********'))
    } else {
      writeOutput(stdout, renderTextPrompt(message, displayValue))
    }
    return recorded
  }

  // Fall back to default if available
  if (defaultValue) {
    writeOutput(stdout, renderTextPrompt(message, defaultValue))
    return defaultValue
  }

  // Real interactive prompt (only when explicitly allowed)
  if (interactive) {
    if (isPassword) {
      return await password({ message })
    }
    return await input({ message, default: defaultValue || undefined })
  }

  // Non-interactive with no answer and no default
  return ''
}

/**
 * Prompt for selection from a list of choices.
 * Resolution: recorded answer → interactive prompt (if allowed) → error.
 */
export async function promptSelect(
  answers: AnswersMap,
  message: string,
  choices: Choice[],
  multiple: boolean = false,
  stdout?: (text: string) => void,
  interactive: boolean = false,
): Promise<JsonValue> {
  const recorded = answers.get(message)
  if (recorded !== undefined) {
    if (multiple) {
      const selectedNames = Array.isArray(recorded)
        ? recorded.map(v => typeof v === 'string' ? v : toDisplayYaml(v))
        : [typeof recorded === 'string' ? recorded : toDisplayYaml(recorded)]
      const selection = choices.filter(c => selectedNames.includes(c.displayName))
      writeOutput(stdout, renderSelectPrompt(message, choices, selection))
      return selection.map(c => c.value)
    } else {
      const answerStr = typeof recorded === 'string' ? recorded : toDisplayYaml(recorded)
      const selection = choices.find(c => c.displayName === answerStr)
      if (!selection) {
        throw new Error(`Prerecorded choice '${answerStr}' not found in provided list.`)
      }
      writeOutput(stdout, renderSelectPrompt(message, choices, [selection]))
      return selection.value
    }
  }

  // Real interactive prompt (only when explicitly allowed)
  if (interactive) {
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

  // Non-interactive with no recorded answer — select prompts can't default
  throw new Error(`No prerecorded answer for '${message}' and not in interactive mode`)
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
