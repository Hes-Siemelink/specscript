import type {JsonObject} from '../language/types.js'

/**
 * Best-effort to strip secrets before printing. Removes the "password" field from an object, if present. Does not recurse into nested objects.
 */
export function withoutSecrets(obj: JsonObject): JsonObject {
    const copy = structuredClone(obj)
    delete copy['password']
    return copy
}