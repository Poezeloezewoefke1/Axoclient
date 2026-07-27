import { mkdtemp, mkdir, readdir, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  addUserMods,
  listUserMods,
  protectedModFiles,
  removeUserMod,
  setUserModEnabled
} from '../src/main/userMods'
import { instanceModsDir } from '../src/main/paths'
import { syncDirectory } from '../src/main/sync'

const VERSION = '1.21.11-r1'

describe('user mods', () => {
  let root: string
  let install: string
  let source: string

  beforeEach(async () => {
    root = await mkdtemp(join(tmpdir(), 'axo-usermods-'))
    install = join(root, 'install')
    source = join(root, 'downloads')
    await mkdir(source, { recursive: true })
    await writeFile(join(source, 'iris.jar'), 'fake jar', 'utf8')
    await writeFile(join(source, 'jei.jar'), 'fake jar', 'utf8')
  })

  afterEach(async () => {
    await rm(root, { recursive: true, force: true })
  })

  it('starts with no mods', async () => {
    expect(await listUserMods(install, VERSION)).toEqual([])
  })

  it('adds jars and records them as enabled', async () => {
    const mods = await addUserMods(install, VERSION, [
      join(source, 'iris.jar'),
      join(source, 'jei.jar')
    ])
    expect(mods).toEqual([
      { fileName: 'iris.jar', enabled: true },
      { fileName: 'jei.jar', enabled: true }
    ])
    expect(await readdir(instanceModsDir(install, VERSION))).toContain('iris.jar')
  })

  it('ignores files that are not jars', async () => {
    await writeFile(join(source, 'notes.txt'), 'hello', 'utf8')
    const mods = await addUserMods(install, VERSION, [join(source, 'notes.txt')])
    expect(mods).toEqual([])
  })

  it('renames a colliding jar rather than overwriting the first', async () => {
    await addUserMods(install, VERSION, [join(source, 'iris.jar')])
    const mods = await addUserMods(install, VERSION, [join(source, 'iris.jar')])
    expect(mods.map((m) => m.fileName)).toEqual(['iris.jar', 'iris-2.jar'])
  })

  it('disabling parks the jar so Fabric ignores it, and re-enabling restores it', async () => {
    await addUserMods(install, VERSION, [join(source, 'iris.jar')])

    const off = await setUserModEnabled(install, VERSION, 'iris.jar', false)
    expect(off).toEqual([{ fileName: 'iris.jar', enabled: false }])
    const namesOff = await readdir(instanceModsDir(install, VERSION))
    expect(namesOff).toContain('iris.jar.disabled')
    expect(namesOff).not.toContain('iris.jar')

    const on = await setUserModEnabled(install, VERSION, 'iris.jar', true)
    expect(on).toEqual([{ fileName: 'iris.jar', enabled: true }])
    expect(await readdir(instanceModsDir(install, VERSION))).toContain('iris.jar')
  })

  it('removing deletes the file in either state', async () => {
    await addUserMods(install, VERSION, [join(source, 'iris.jar'), join(source, 'jei.jar')])
    await setUserModEnabled(install, VERSION, 'jei.jar', false)

    expect(await removeUserMod(install, VERSION, 'iris.jar')).toEqual([
      { fileName: 'jei.jar', enabled: false }
    ])
    expect(await removeUserMod(install, VERSION, 'jei.jar')).toEqual([])
    const left = await readdir(instanceModsDir(install, VERSION))
    expect(left.filter((n) => n.startsWith('jei'))).toEqual([])
  })

  it('forgets mods whose file was deleted outside the launcher', async () => {
    await addUserMods(install, VERSION, [join(source, 'iris.jar')])
    await rm(join(instanceModsDir(install, VERSION), 'iris.jar'))
    expect(await listUserMods(install, VERSION)).toEqual([])
  })

  it('keeps each version separate', async () => {
    await addUserMods(install, VERSION, [join(source, 'iris.jar')])
    expect(await listUserMods(install, 'other-version')).toEqual([])
  })

  // The point of the whole registry: sync deletes stray jars, and a user's
  // mod is by definition not in the manifest.
  it('survives a sync that removes stray jars', async () => {
    await addUserMods(install, VERSION, [join(source, 'iris.jar')])
    const modsDir = instanceModsDir(install, VERSION)
    await writeFile(join(modsDir, 'leftover-old-axo.jar'), 'stale', 'utf8')

    const result = await syncDirectory(
      modsDir,
      [],
      undefined,
      undefined,
      await protectedModFiles(install, VERSION)
    )

    expect(result.removed).toEqual(['leftover-old-axo.jar'])
    expect(await readdir(modsDir)).toContain('iris.jar')
    expect(await listUserMods(install, VERSION)).toEqual([{ fileName: 'iris.jar', enabled: true }])
  })

  it('a disabled mod is untouched by sync too', async () => {
    await addUserMods(install, VERSION, [join(source, 'iris.jar')])
    await setUserModEnabled(install, VERSION, 'iris.jar', false)
    const modsDir = instanceModsDir(install, VERSION)

    await syncDirectory(modsDir, [], undefined, undefined, await protectedModFiles(install, VERSION))

    expect(await readdir(modsDir)).toContain('iris.jar.disabled')
  })
})
