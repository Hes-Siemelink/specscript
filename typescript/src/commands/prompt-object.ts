/**
 * Prompt object command: asks multiple questions at once.
 *
 * DelayedResolver — variables in properties are NOT resolved upfront by the engine.
 * Instead, each property is resolved manually using the growing variables map,
 * enabling dependent questions where later prompts reference earlier answers.
 */

import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isString } from '../language/types.js'
import { resolveVariables } from '../language/variables.js'
import { doPrompt } from './prompt.js'

export const PromptObjectCommand: CommandHandler = {
  name: 'Prompt object',
  delayedResolver: true,

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) return undefined

    const result: JsonObject = {}

    // Build a variables map starting from context variables, growing as answers accumulate
    const variables = new Map(context.variables)

    for (const [name, parameterData] of Object.entries(data as JsonObject)) {
      // Resolve variables in this parameter definition using current state
      const resolved = resolveVariables(parameterData, variables)

      // Parse into a prompt definition
      let def: JsonObject
      if (isString(resolved)) {
        def = { description: resolved }
      } else if (isObject(resolved)) {
        def = resolved as JsonObject
      } else {
        continue
      }

      // Prompt (handles condition checking, answer lookup, etc.)
      const answer = await doPrompt(context, def, name)
      if (answer === null) continue  // condition was false

      // Add answer to result and to variables for dependent questions
      result[name] = answer
      variables.set(name, answer)
    }

    return result
  },
}
