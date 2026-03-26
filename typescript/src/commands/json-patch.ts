import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue } from '../language/types.js'
import { isObject, isArray, SpecScriptCommandError, CommandFormatError } from '../language/types.js'
import type { ScriptContext } from '../language/context.js'
import { applyJsonPatch } from '../util/json-patch.js'

/**
 * Json patch: applies RFC 6902 JSON Patch operations to a document.
 */
export const JsonPatchCommand: CommandHandler = {
  name: 'Json patch',

  execute(data: JsonValue, context: ScriptContext): JsonValue {
    if (!isObject(data)) {
      throw new CommandFormatError('Json patch expects an object')
    }
    const doc = data['doc'] ?? context.output
    if (doc === undefined) {
      throw new SpecScriptCommandError(
        "Json patch needs 'doc' parameter or non-null output variable."
      )
    }
    const patch = data['patch']
    if (!isArray(patch)) {
      throw new CommandFormatError("Json patch requires a 'patch' array")
    }
    return applyJsonPatch(doc, patch)
  },
}
