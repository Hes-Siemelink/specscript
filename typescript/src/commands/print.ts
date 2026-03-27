import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue } from '../language/types.js'
import { toDisplayString } from '../language/types.js'
import type { ScriptContext } from '../language/context.js'
import { toDisplayYaml } from '../util/yaml.js'

/**
 * Print: writes a value to stdout.
 */
export const Print: CommandHandler = {
  name: 'Print',

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    const text = typeof data === 'string' ? data : toDisplayYaml(data)
    const writer = context.session.get('stdout') as ((s: string) => void) | undefined
    if (writer) {
      writer(text)
    } else {
      console.log(text)
    }
    return undefined
  },
}
