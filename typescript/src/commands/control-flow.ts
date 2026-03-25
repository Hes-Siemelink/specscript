import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue } from '../language/types.js'
import { Break } from '../language/types.js'
import type { ScriptContext } from '../language/context.js'
import { Script } from '../language/script.js'
import { resolveVariables } from '../language/variables.js'

/**
 * Do: executes a sub-script (a block of commands).
 */
export const Do: CommandHandler = {
  name: 'Do',
  delayedResolver: true,
  handlesLists: true,

  execute(data: JsonValue, context: ScriptContext): JsonValue | undefined {
    const script = Script.fromData(data)
    return script.run(context)
  },
}

/**
 * Exit: terminates the current script and returns a value.
 */
export const Exit: CommandHandler = {
  name: 'Exit',

  execute(data: JsonValue, _context: ScriptContext): JsonValue | undefined {
    throw new Break(data)
  },
}
