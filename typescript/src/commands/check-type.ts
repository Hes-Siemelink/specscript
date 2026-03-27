/**
 * Check type command — validates data against SpecScript type definitions.
 *
 * Mirrors Kotlin: specscript.commands.types.CheckType
 */

import type { CommandHandler } from '../language/command-handler.js'
import type { JsonValue } from '../language/types.js'
import { isObject, SpecScriptCommandError, CommandFormatError } from '../language/types.js'
import type { ScriptContext } from '../language/context.js'
import { parseTypeSpecification, resolveType, validateType } from '../language/type-system.js'

export const CheckTypeCommand: CommandHandler = {
  name: 'Check type',

  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (!isObject(data)) {
      throw new CommandFormatError('Check type requires an object with "item" and "type"')
    }

    const item = data['item']
    if (item === undefined) {
      throw new CommandFormatError('Check type requires an "item" property')
    }

    const typeData = data['type']
    if (typeData === undefined) {
      throw new CommandFormatError('Check type requires a "type" property')
    }

    // Parse and resolve the type specification
    const typeSpec = parseTypeSpecification(typeData)
    const resolvedType = resolveType(typeSpec, context.types)

    // Validate the data against the resolved type
    const messages = validateType(item, resolvedType.definition)
    if (messages.length > 0) {
      throw new SpecScriptCommandError(
        'Type validation errors',
        'Type validation',
        messages
      )
    }

    return undefined
  },
}
