import {describe, expect, it} from 'vitest'
import {getDefaultProperty} from '../src/commands/script-info.js'

describe('x-default-property validation', () => {
    it('returns the named scalar property', () => {
        expect(getDefaultProperty({
            'x-default-property': 'name',
            properties: {name: {type: 'string'}},
        })).toBe('name')
    })

    it('returns undefined when not declared', () => {
        expect(getDefaultProperty({properties: {name: {}}})).toBeUndefined()
    })

    it('throws when the property is missing', () => {
        expect(() => getDefaultProperty({
            'x-default-property': 'missing',
            properties: {name: {type: 'string'}},
        })).toThrow()
    })

    it('throws when the property is not scalar', () => {
        expect(() => getDefaultProperty({
            'x-default-property': 'config',
            properties: {config: {type: 'object'}},
        })).toThrow()
    })
})
