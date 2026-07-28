import { describe, expect, it } from 'vitest'
import {
  PROFILE_LIMITS,
  findProfile,
  normalizeName,
  parseProfiles,
  profilePatch,
  removeProfile,
  sameName,
  serializeProfiles,
  upsertProfile
} from '../src/main/profiles'
import type { LaunchProfile } from '../src/shared/types'

const pvp: LaunchProfile = { name: 'PvP', ramMb: 4096, jvmArgs: '-XX:+UseG1GC', channel: 'stable' }

describe('normalizeName', () => {
  it('trims and collapses whitespace', () => {
    expect(normalizeName('  My   Profile ')).toBe('My Profile')
  })

  it('caps the length', () => {
    expect(normalizeName('x'.repeat(200))).toHaveLength(PROFILE_LIMITS.maxNameLength)
  })
})

describe('sameName', () => {
  it('ignores case', () => {
    expect(sameName('PvP', 'pvp')).toBe(true)
    expect(sameName('PvP', 'Survival')).toBe(false)
  })
})

describe('parseProfiles', () => {
  it('reads a wrapped list', () => {
    expect(parseProfiles(serializeProfiles([pvp]))).toEqual([pvp])
  })

  it('reads a bare array too', () => {
    expect(parseProfiles(JSON.stringify([pvp]))).toEqual([pvp])
  })

  it('returns empty for junk', () => {
    expect(parseProfiles('not json')).toEqual([])
    expect(parseProfiles('{"profiles":"nope"}')).toEqual([])
  })

  it('drops entries with no usable name', () => {
    expect(parseProfiles(JSON.stringify([{ name: '   ' }, { ramMb: 2048 }]))).toEqual([])
  })

  it('drops duplicates that differ only by case', () => {
    const parsed = parseProfiles(JSON.stringify([{ name: 'PvP' }, { name: 'pvp', ramMb: 8192 }]))
    expect(parsed).toHaveLength(1)
    expect(parsed[0].name).toBe('PvP')
  })

  it('clamps out-of-range RAM', () => {
    const [low] = parseProfiles(JSON.stringify([{ name: 'a', ramMb: 1 }]))
    const [high] = parseProfiles(JSON.stringify([{ name: 'b', ramMb: 999999 }]))
    expect(low.ramMb).toBe(1024)
    expect(high.ramMb).toBe(16384)
  })

  it('leaves unset fields undefined rather than defaulting them', () => {
    const [only] = parseProfiles(JSON.stringify([{ name: 'RAM only', ramMb: 4096 }]))
    expect(only.jvmArgs).toBeUndefined()
    expect(only.channel).toBeUndefined()
  })
})

describe('upsertProfile', () => {
  it('appends a new profile', () => {
    expect(upsertProfile([], pvp)).toEqual([pvp])
  })

  it('replaces by name, case-insensitively, in place', () => {
    const next = upsertProfile([pvp, { name: 'Survival' }], { name: 'pvp', ramMb: 8192 })
    expect(next).toHaveLength(2)
    expect(next[0]).toEqual({
      name: 'pvp',
      ramMb: 8192,
      jvmArgs: undefined,
      channel: undefined,
      versionId: undefined
    })
    expect(next[1].name).toBe('Survival')
  })

  it('does not mutate the input list', () => {
    const original = [pvp]
    upsertProfile(original, { name: 'Survival' })
    expect(original).toHaveLength(1)
  })

  it('refuses to grow past the cap', () => {
    const full = Array.from({ length: PROFILE_LIMITS.maxProfiles }, (_, i) => ({ name: `p${i}` }))
    expect(upsertProfile(full, { name: 'one more' })).toHaveLength(PROFILE_LIMITS.maxProfiles)
  })

  it('ignores a nameless profile', () => {
    expect(upsertProfile([pvp], { name: '  ' })).toEqual([pvp])
  })
})

describe('removeProfile / findProfile', () => {
  it('removes case-insensitively', () => {
    expect(removeProfile([pvp], 'PVP')).toEqual([])
  })

  it('leaves the list alone when the name is unknown', () => {
    expect(removeProfile([pvp], 'nope')).toEqual([pvp])
  })

  it('finds case-insensitively', () => {
    expect(findProfile([pvp], 'pVp')?.name).toBe('PvP')
    expect(findProfile([pvp], 'other')).toBeUndefined()
  })
})

describe('profilePatch', () => {
  it('includes only the fields the profile pins', () => {
    expect(profilePatch({ name: 'RAM only', ramMb: 4096 })).toEqual({ ramMb: 4096 })
  })

  it('carries every set field through', () => {
    expect(profilePatch(pvp)).toEqual({ ramMb: 4096, jvmArgs: '-XX:+UseG1GC', channel: 'stable' })
  })

  it('keeps an intentionally empty jvmArgs (it means "clear them")', () => {
    expect(profilePatch({ name: 'clean', jvmArgs: '' })).toEqual({ jvmArgs: '' })
  })
})
