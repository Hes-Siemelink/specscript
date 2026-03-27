/**
 * HTTP verb commands: GET, POST, PUT, PATCH, DELETE, Http request defaults.
 *
 * Thin wrappers that delegate to the shared HttpClient module.
 * Mirrors Kotlin's Get.kt, Post.kt, Put.kt, Patch.kt, Delete.kt, HttpRequestDefaults.kt.
 */

import type { CommandHandler } from '../language/command-handler.js'
import type { ScriptContext } from '../language/context.js'
import type { JsonValue, JsonObject } from '../language/types.js'
import { isObject, isString, CommandFormatError } from '../language/types.js'
import { processValueRequest, processObjectRequest, storeDefaults, getDefaults } from './http-client.js'

function createHttpVerbCommand(name: string, method: string, supportsValue: boolean): CommandHandler {
  return {
    name,
    async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
      if (supportsValue && isString(data)) {
        return processValueRequest(data, context, method)
      }
      if (isObject(data)) {
        return processObjectRequest(data, context, method)
      }
      if (supportsValue) {
        throw new CommandFormatError(`${name}: expected a URL string or object`)
      }
      throw new CommandFormatError(`${name}: expected an object`)
    },
  }
}

export const GetCommand = createHttpVerbCommand('GET', 'GET', true)
export const PostCommand = createHttpVerbCommand('POST', 'POST', true)
export const PutCommand = createHttpVerbCommand('PUT', 'PUT', false)
export const PatchCommand = createHttpVerbCommand('PATCH', 'PATCH', false)
export const DeleteCommand = createHttpVerbCommand('DELETE', 'DELETE', true)

/** Http request defaults — store/retrieve session-scoped defaults */
export const HttpRequestDefaultsCommand: CommandHandler = {
  name: 'Http request defaults',
  async execute(data: JsonValue, context: ScriptContext): Promise<JsonValue | undefined> {
    if (isObject(data)) {
      storeDefaults(context, data)
      return undefined
    }
    // Value form: retrieve current defaults
    return getDefaults(context) ?? {}
  },
}
