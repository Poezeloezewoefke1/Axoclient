import { describe, expect, it } from 'vitest'
import { compareSemver, parseSemver } from '../src/main/semver'

describe('parseSemver', () => {
  it('parses X.Y.Z', () => {
    expect(parseSemver('1.2.3')).toEqual([1, 2, 3])
    expect(parseSemver('0.1.0')).toEqual([0, 1, 0])
  })

  it('returns null for junk', () => {
    expect(parseSemver('abc')).toBeNull()
    expect(parseSemver('')).toBeNull()
  })
})

describe('compareSemver', () => {
  it('orders correctly across positions', () => {
    expect(compareSemver('0.1.0', '0.1.0')).toBe(0)
    expect(compareSemver('0.1.0', '0.1.1')).toBe(-1)
    expect(compareSemver('0.2.0', '0.1.9')).toBe(1)
    expect(compareSemver('1.0.0', '0.99.99')).toBe(1)
    expect(compareSemver('0.9.0', '0.10.0')).toBe(-1)
  })
})
