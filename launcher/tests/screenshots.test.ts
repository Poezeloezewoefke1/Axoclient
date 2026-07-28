import { mkdir, mkdtemp, rm, utimes, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  deleteScreenshot,
  isSafeScreenshotName,
  listScreenshots,
  readScreenshot,
  screenshotPath,
  screenshotsDir
} from '../src/main/screenshots'

const VERSION = '1.21.11-r1'

describe('isSafeScreenshotName', () => {
  it('accepts ordinary image names', () => {
    expect(isSafeScreenshotName('2026-07-27_12.00.00.png')).toBe(true)
    expect(isSafeScreenshotName('shot.JPEG')).toBe(true)
  })

  it('rejects anything that could escape the folder', () => {
    expect(isSafeScreenshotName('../../secret.png')).toBe(false)
    expect(isSafeScreenshotName('sub/dir.png')).toBe(false)
    expect(isSafeScreenshotName('sub\\dir.png')).toBe(false)
    expect(isSafeScreenshotName('..')).toBe(false)
    expect(isSafeScreenshotName('bad\0.png')).toBe(false)
  })

  it('rejects non-images and empty names', () => {
    expect(isSafeScreenshotName('notes.txt')).toBe(false)
    expect(isSafeScreenshotName('options.json')).toBe(false)
    expect(isSafeScreenshotName('')).toBe(false)
  })
})

describe('screenshot listing', () => {
  let install: string
  let dir: string

  beforeEach(async () => {
    install = await mkdtemp(join(tmpdir(), 'axo-shots-'))
    dir = screenshotsDir(install, VERSION)
    await mkdir(dir, { recursive: true })
  })

  afterEach(async () => {
    await rm(install, { recursive: true, force: true })
  })

  it('returns nothing when the folder is missing', async () => {
    expect(await listScreenshots(install, 'no-such-version')).toEqual([])
  })

  it('lists images newest first and ignores other files', async () => {
    await writeFile(join(dir, 'old.png'), 'a')
    await writeFile(join(dir, 'new.png'), 'bb')
    await writeFile(join(dir, 'notes.txt'), 'ignore me')
    const past = new Date(Date.now() - 60_000)
    await utimes(join(dir, 'old.png'), past, past)

    const shots = await listScreenshots(install, VERSION)
    expect(shots.map((s) => s.fileName)).toEqual(['new.png', 'old.png'])
    expect(shots[0].sizeBytes).toBe(2)
  })

  it('reads an image as a data URL', async () => {
    await writeFile(join(dir, 'shot.png'), 'binary')
    const url = await readScreenshot(install, VERSION, 'shot.png')
    expect(url?.startsWith('data:image/png;base64,')).toBe(true)
  })

  it('refuses to read outside the screenshots folder', async () => {
    await writeFile(join(install, 'secret.png'), 'nope')
    expect(await readScreenshot(install, VERSION, '../secret.png')).toBeNull()
    expect(screenshotPath(install, VERSION, '../secret.png')).toBeNull()
  })

  it('deletes a screenshot and returns the rest', async () => {
    await writeFile(join(dir, 'a.png'), 'a')
    await writeFile(join(dir, 'b.png'), 'b')
    const left = await deleteScreenshot(install, VERSION, 'a.png')
    expect(left.map((s) => s.fileName)).toEqual(['b.png'])
  })

  it('a traversal path deletes nothing', async () => {
    await writeFile(join(install, 'secret.png'), 'keep me')
    await writeFile(join(dir, 'a.png'), 'a')
    const left = await deleteScreenshot(install, VERSION, '../secret.png')
    expect(left.map((s) => s.fileName)).toEqual(['a.png'])
  })
})
