import {
  type JsonValue, type JsonObject,
  isObject, isArray, isString, isNumber,
  CommandFormatError,
} from './types.js'

/**
 * A condition that can be evaluated to true or false.
 */
export interface Condition {
  isTrue(): boolean
}

/**
 * Parse a JSON value into a Condition tree.
 */
export function toCondition(node: JsonValue): Condition {
  if (!isObject(node)) {
    throw new CommandFormatError(`Condition must be an object, got ${typeof node}`)
  }

  // all: [...conditions]
  if ('all' in node) {
    const items = node['all']
    if (!isArray(items)) throw new CommandFormatError(`'all' must be an array`)
    return new All(items.map(toCondition))
  }

  // any: [...conditions]
  if ('any' in node) {
    const items = node['any']
    if (!isArray(items)) throw new CommandFormatError(`'any' must be an array`)
    return new Any(items.map(toCondition))
  }

  // not: condition
  if ('not' in node) {
    return new Not(toCondition(node['not']))
  }

  // empty: value
  if ('empty' in node) {
    return new Empty(node['empty'])
  }

  // item + operator
  if ('item' in node) {
    const item = node['item']

    if ('equals' in node) return new Equals(item, node['equals'])
    if ('is' in node) return new Equals(item, node['is'])
    if ('is not' in node) return new Not(new Equals(item, node['is not']))
    if ('in' in node) return new Contains(node['in'], item)
    if ('contains' in node) return new Contains(item, node['contains'])
    if ('matches' in node) return new Matches(item, node['matches'])
    if ('greater than' in node) return new GreaterThan(item, node['greater than'])
    if ('less than' in node) return new LessThan(item, node['less than'])
    if ('greater or equal' in node) return new GreaterOrEqual(item, node['greater or equal'])
    if ('less or equal' in node) return new LessOrEqual(item, node['less or equal'])
  }

  throw new CommandFormatError(`Unrecognized condition format: ${JSON.stringify(node)}`)
}

// --- Condition implementations ---

class Equals implements Condition {
  constructor(private left: JsonValue, private right: JsonValue) {}

  isTrue(): boolean {
    return deepEquals(this.left, this.right)
  }
}

class Contains implements Condition {
  constructor(private container: JsonValue, private item: JsonValue) {}

  isTrue(): boolean {
    // Array: check membership
    if (isArray(this.container)) {
      return this.container.some(element => deepEquals(element, this.item))
    }
    // String: substring check
    if (isString(this.container) && isString(this.item)) {
      return this.container.includes(this.item)
    }
    // Object: subset check — all key-value pairs of item exist in container
    if (isObject(this.container) && isObject(this.item)) {
      const container = this.container
      return Object.entries(this.item).every(
        ([key, value]) => key in container && deepEquals(container[key], value)
      )
    }
    return false
  }
}

class Matches implements Condition {
  constructor(private item: JsonValue, private pattern: JsonValue) {}

  isTrue(): boolean {
    if (!isString(this.item) || !isString(this.pattern)) return false
    try {
      return new RegExp(this.pattern).test(this.item)
    } catch {
      throw new CommandFormatError(`Invalid regex pattern: ${this.pattern}`)
    }
  }
}

class All implements Condition {
  constructor(private conditions: Condition[]) {}

  isTrue(): boolean {
    return this.conditions.every(c => c.isTrue())
  }
}

class Any implements Condition {
  constructor(private conditions: Condition[]) {}

  isTrue(): boolean {
    return this.conditions.some(c => c.isTrue())
  }
}

class Not implements Condition {
  constructor(private condition: Condition) {}

  isTrue(): boolean {
    return !this.condition.isTrue()
  }
}

class Empty implements Condition {
  constructor(private value: JsonValue) {}

  isTrue(): boolean {
    if (this.value === null) return true
    if (isString(this.value)) return this.value.length === 0
    if (isArray(this.value)) return this.value.length === 0
    if (isObject(this.value)) return Object.keys(this.value).length === 0
    if (isNumber(this.value)) return this.value === 0
    return false
  }
}

class GreaterThan implements Condition {
  constructor(private left: JsonValue, private right: JsonValue) {}
  isTrue(): boolean {
    if (isNumber(this.left) && isNumber(this.right)) return this.left > this.right
    if (isString(this.left) && isString(this.right)) return this.left > this.right
    return false
  }
}

class LessThan implements Condition {
  constructor(private left: JsonValue, private right: JsonValue) {}
  isTrue(): boolean {
    if (isNumber(this.left) && isNumber(this.right)) return this.left < this.right
    if (isString(this.left) && isString(this.right)) return this.left < this.right
    return false
  }
}

class GreaterOrEqual implements Condition {
  constructor(private left: JsonValue, private right: JsonValue) {}
  isTrue(): boolean {
    if (isNumber(this.left) && isNumber(this.right)) return this.left >= this.right
    if (isString(this.left) && isString(this.right)) return this.left >= this.right
    return false
  }
}

class LessOrEqual implements Condition {
  constructor(private left: JsonValue, private right: JsonValue) {}
  isTrue(): boolean {
    if (isNumber(this.left) && isNumber(this.right)) return this.left <= this.right
    if (isString(this.left) && isString(this.right)) return this.left <= this.right
    return false
  }
}

/**
 * Deep structural equality for JSON values.
 */
export function deepEquals(a: JsonValue, b: JsonValue): boolean {
  if (a === b) return true
  if (a === null || b === null) return false
  if (typeof a !== typeof b) return false

  if (isArray(a) && isArray(b)) {
    if (a.length !== b.length) return false
    return a.every((item, i) => deepEquals(item, b[i]))
  }

  if (isObject(a) && isObject(b)) {
    const keysA = Object.keys(a)
    const keysB = Object.keys(b)
    if (keysA.length !== keysB.length) return false
    return keysA.every(key => key in b && deepEquals(a[key], b[key]))
  }

  return false
}
