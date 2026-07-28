import { mkdtemp, readdir, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  deleteSavedSkin,
  listSavedSkins,
  parseIndex,
  saveSkinData,
  saveSkinFile,
  savedSkinPath,
  toSkinId,
  uniqueId
} from '../src/main/skinLibrary'

/** Smallest valid PNG payload; contents don't matter, only that bytes survive. */
const PNG_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='
const PNG_DATA_URL = `data:image/png;base64,${PNG_BASE64}`

describe('toSkinId', () => {
  it('makes a filesystem-safe slug', () => {
    expect(toSkinId('My Cool Skin')).toBe('my-cool-skin')
    expect(toSkinId('  Spaces  ')).toBe('spaces')
    expect(toSkinId('weird/\\:*?chars')).toBe('weird-chars')
  })

  it('falls back when a name has nothing usable', () => {
    expect(toSkinId('***')).toBe('skin')
    expect(toSkinId('')).toBe('skin')
  })

  it('caps very long names', () => {
    expect(toSkinId('a'.repeat(200)).length).toBeLessThanOrEqual(40)
  })
})

describe('uniqueId', () => {
  it('returns the base when free', () => {
    expect(uniqueId('skin', new Set())).toBe('skin')
  })

  it('walks past taken ids', () => {
    expect(uniqueId('skin', new Set(['skin']))).toBe('skin-2')
    expect(uniqueId('skin', new Set(['skin', 'skin-2', 'skin-3']))).toBe('skin-4')
  })
})

describe('parseIndex', () => {
  it('returns nothing for missing or corrupt data', () => {
    expect(parseIndex(null)).toEqual([])
    expect(parseIndex('{oops')).toEqual([])
    expect(parseIndex('{"not":"an array"}')).toEqual([])
  })

  it('drops malformed entries and normalises slim', () => {
    const raw = JSON.stringify([
      { id: 'a', name: 'A', slim: true },
      { id: 'b', name: 'B' },
      { id: '', name: 'bad' },
      { nonsense: 1 }
    ])
    expect(parseIndex(raw)).toEqual([
      { id: 'a', name: 'A', slim: true },
      { id: 'b', name: 'B', slim: false }
    ])
  })
})

describe('skin library on disk', () => {
  let dir: string
  let source: string

  beforeEach(async () => {
    const root = await mkdtemp(join(tmpdir(), 'axo-skins-'))
    dir = join(root, 'skins')
    source = join(root, 'alex.png')
    await writeFile(source, Buffer.from(PNG_BASE64, 'base64'))
  })

  afterEach(async () => {
    await rm(dir, { recursive: true, force: true })
  })

  it('starts empty', async () => {
    expect(await listSavedSkins(dir)).toEqual([])
  })

  it('saves a file and returns it as a data URL', async () => {
    const skins = await saveSkinFile(dir, source, 'My Skin', true)
    expect(skins).toHaveLength(1)
    expect(skins[0]).toMatchObject({ id: 'my-skin', name: 'My Skin', slim: true })
    expect(skins[0].dataUrl.startsWith('data:image/png;base64,')).toBe(true)
  })

  it('saves in-memory skin data', async () => {
    const skins = await saveSkinData(dir, PNG_DATA_URL, 'Current', false)
    expect(skins[0]).toMatchObject({ id: 'current', slim: false })
    expect(await readdir(dir)).toContain('current.png')
  })

  it('rejects data that is not a PNG data URL', async () => {
    await expect(saveSkinData(dir, 'https://example.invalid/x.png', 'x', false)).rejects.toThrow(
      /not a PNG/i
    )
  })

  it('keeps both when two skins share a name', async () => {
    await saveSkinFile(dir, source, 'Twin', false)
    const skins = await saveSkinFile(dir, source, 'Twin', false)
    expect(skins.map((s) => s.id)).toEqual(['twin', 'twin-2'])
    expect(skins.every((s) => s.name === 'Twin')).toBe(true)
  })

  it('deletes a skin and its file', async () => {
    await saveSkinFile(dir, source, 'Gone', false)
    expect(await deleteSavedSkin(dir, 'gone')).toEqual([])
    expect(await readdir(dir)).not.toContain('gone.png')
  })

  it('forgets entries whose PNG was deleted outside the launcher', async () => {
    await saveSkinFile(dir, source, 'Ghost', false)
    await rm(join(dir, 'ghost.png'))
    expect(await listSavedSkins(dir)).toEqual([])
  })

  it('exposes the path used for uploading', () => {
    expect(savedSkinPath('/tmp/skins', 'abc')).toBe(join('/tmp/skins', 'abc.png'))
  })
})
