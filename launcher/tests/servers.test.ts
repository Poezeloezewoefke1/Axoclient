import { describe, expect, it } from 'vitest'
import {
  SERVER_LIMITS,
  isValidAddress,
  normalizeAddress,
  normalizeName,
  parseServers,
  removeServer,
  serializeServers,
  upsertServer
} from '../src/main/servers'
import type { SavedServer } from '../src/shared/types'

const hypixel: SavedServer = { name: 'Hypixel', address: 'mc.hypixel.net' }

describe('normalizeAddress', () => {
  it('trims, lowercases and strips inner whitespace', () => {
    expect(normalizeAddress('  MC.Hypixel.Net ')).toBe('mc.hypixel.net')
    expect(normalizeAddress('play. example .net')).toBe('play.example.net')
  })

  it('caps the length', () => {
    expect(normalizeAddress('a'.repeat(400))).toHaveLength(SERVER_LIMITS.maxAddressLength)
  })
})

describe('isValidAddress', () => {
  it('accepts hostnames, IPv4 and explicit ports', () => {
    expect(isValidAddress('mc.hypixel.net')).toBe(true)
    expect(isValidAddress('192.168.1.20')).toBe(true)
    expect(isValidAddress('play.example.net:25566')).toBe(true)
    expect(isValidAddress('my_server-1.local')).toBe(true)
  })

  it('rejects empty input', () => {
    expect(isValidAddress('')).toBe(false)
    expect(isValidAddress('   ')).toBe(false)
  })

  it('rejects a pasted URL', () => {
    expect(isValidAddress('https://mc.hypixel.net')).toBe(false)
    expect(isValidAddress('mc.hypixel.net/join')).toBe(false)
    expect(isValidAddress('user@mc.hypixel.net')).toBe(false)
  })

  it('rejects a bad port', () => {
    expect(isValidAddress('host:0')).toBe(false)
    expect(isValidAddress('host:70000')).toBe(false)
    expect(isValidAddress('host:abc')).toBe(false)
    expect(isValidAddress('host:25565:1')).toBe(false)
  })

  it('rejects a missing host', () => {
    expect(isValidAddress(':25565')).toBe(false)
  })
})

describe('normalizeName', () => {
  it('collapses whitespace and caps length', () => {
    expect(normalizeName('  My   Server ')).toBe('My Server')
    expect(normalizeName('x'.repeat(100))).toHaveLength(SERVER_LIMITS.maxNameLength)
  })
})

describe('parseServers', () => {
  it('round-trips through serialize', () => {
    expect(parseServers(serializeServers([hypixel]))).toEqual([hypixel])
  })

  it('reads a bare array too', () => {
    expect(parseServers(JSON.stringify([hypixel]))).toEqual([hypixel])
  })

  it('returns empty for junk', () => {
    expect(parseServers('nope')).toEqual([])
    expect(parseServers('{"servers":42}')).toEqual([])
  })

  it('drops entries with an unusable address', () => {
    expect(parseServers(JSON.stringify([{ name: 'Bad', address: 'https://x.net' }]))).toEqual([])
    expect(parseServers(JSON.stringify([{ name: 'No address' }]))).toEqual([])
  })

  it('falls back to the address when there is no name', () => {
    const [only] = parseServers(JSON.stringify([{ address: 'mc.hypixel.net' }]))
    expect(only.name).toBe('mc.hypixel.net')
  })

  it('deduplicates by address, keeping the first', () => {
    const parsed = parseServers(
      JSON.stringify([hypixel, { name: 'Duplicate', address: 'MC.HYPIXEL.NET' }])
    )
    expect(parsed).toHaveLength(1)
    expect(parsed[0].name).toBe('Hypixel')
  })

  it('stops at the cap', () => {
    const many = Array.from({ length: 60 }, (_, i) => ({ name: `s${i}`, address: `h${i}.net` }))
    expect(parseServers(JSON.stringify(many))).toHaveLength(SERVER_LIMITS.maxServers)
  })
})

describe('upsertServer', () => {
  it('appends a new server', () => {
    expect(upsertServer([], hypixel)).toEqual([hypixel])
  })

  it('renames in place when the address already exists', () => {
    const next = upsertServer([hypixel], { name: 'Hypixel SkyBlock', address: 'mc.hypixel.net' })
    expect(next).toHaveLength(1)
    expect(next[0].name).toBe('Hypixel SkyBlock')
  })

  it('does not mutate the input', () => {
    const original = [hypixel]
    upsertServer(original, { name: 'Other', address: 'other.net' })
    expect(original).toHaveLength(1)
  })

  it('ignores an invalid address', () => {
    expect(upsertServer([hypixel], { name: 'Bad', address: 'https://x' })).toEqual([hypixel])
  })

  it('refuses to grow past the cap', () => {
    const full = Array.from({ length: SERVER_LIMITS.maxServers }, (_, i) => ({
      name: `s${i}`,
      address: `h${i}.net`
    }))
    expect(upsertServer(full, { name: 'extra', address: 'extra.net' })).toHaveLength(
      SERVER_LIMITS.maxServers
    )
  })
})

describe('removeServer', () => {
  it('removes regardless of casing or padding', () => {
    expect(removeServer([hypixel], '  MC.Hypixel.Net ')).toEqual([])
  })

  it('leaves the list alone for an unknown address', () => {
    expect(removeServer([hypixel], 'other.net')).toEqual([hypixel])
  })
})
