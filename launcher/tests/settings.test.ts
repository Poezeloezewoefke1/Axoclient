import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { SETTINGS_LIMITS, SettingsStore } from '../src/main/settings'
import type { AxoSettings } from '../src/shared/types'

const DEFAULTS: AxoSettings = {
  ramMb: 4096,
  channel: 'stable',
  installDir: 'C:/fake/.axoclient',
  jvmArgs: ''
}

describe('SettingsStore', () => {
  let dir: string
  let file: string

  beforeEach(async () => {
    dir = await mkdtemp(join(tmpdir(), 'axo-settings-'))
    file = join(dir, 'settings.json')
  })

  afterEach(async () => {
    await rm(dir, { recursive: true, force: true })
  })

  it('returns defaults when no file exists', async () => {
    const store = new SettingsStore(file, DEFAULTS)
    expect(await store.load()).toEqual(DEFAULTS)
  })

  it('persists updates and reloads them', async () => {
    const store = new SettingsStore(file, DEFAULTS)
    await store.load()
    await store.update({ ramMb: 6144, jvmArgs: '-XX:+UseG1GC' })

    const reloaded = new SettingsStore(file, DEFAULTS)
    const settings = await reloaded.load()
    expect(settings.ramMb).toBe(6144)
    expect(settings.jvmArgs).toBe('-XX:+UseG1GC')
    expect(settings.channel).toBe('stable')
  })

  it('clamps RAM to the allowed range', async () => {
    const store = new SettingsStore(file, DEFAULTS)
    await store.load()
    expect((await store.update({ ramMb: 99 })).ramMb).toBe(SETTINGS_LIMITS.minRamMb)
    expect((await store.update({ ramMb: 999999 })).ramMb).toBe(SETTINGS_LIMITS.maxRamMb)
  })

  it('falls back to defaults on a corrupt file', async () => {
    await writeFile(file, '{not json', 'utf8')
    const store = new SettingsStore(file, DEFAULTS)
    expect(await store.load()).toEqual(DEFAULTS)
  })

  it('ignores junk fields and wrong types on disk', async () => {
    await writeFile(
      file,
      JSON.stringify({ ramMb: 'lots', channel: '', evil: true, jvmArgs: 42 }),
      'utf8'
    )
    const store = new SettingsStore(file, DEFAULTS)
    const settings = await store.load()
    expect(settings).toEqual(DEFAULTS)
  })

  it('writes valid JSON to disk', async () => {
    const store = new SettingsStore(file, DEFAULTS)
    await store.load()
    await store.update({ channel: 'beta' })
    const onDisk = JSON.parse(await readFile(file, 'utf8'))
    expect(onDisk.channel).toBe('beta')
  })
})
