/**
 * Confirm command: asks the user for yes/no confirmation.
 *
 * Presents a Yes/No choice. Returns "Yes" on confirmation.
 * Throws SpecScriptCommandError on rejection ("No").
 */

import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue } from '../language/types.js'
import { isString, SpecScriptCommandError } from '../language/types.js'
import { promptSelect, getAnswers } from '../language/user-prompt.js'
import type { Choice } from '../language/user-prompt.js'

export const ConfirmCommand: CommandHandler = {
  name: 'Confirm',

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isString(data)) return undefined

    const message = data
    const choices: Choice[] = [
      { displayName: 'Yes', value: 'Yes' },
      { displayName: 'No', value: 'No' },
    ]

    const answers = getAnswers(context.session)
    const stdout = context.session.get('stdout') as ((text: string) => void) | undefined

    const answer = await promptSelect(answers, message, choices, false, stdout, context.interactive)

    if (answer === 'No') {
      throw new SpecScriptCommandError(
        'No confirmation -- action canceled.',
        'Confirmation',
      )
    }

    return answer
  },
}
