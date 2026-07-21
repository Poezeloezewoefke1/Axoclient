import { createHash } from 'node:crypto'
import { mkdir, mkdtemp, rm, stat, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { instanceModsDir, instanceDir } from '../src/main/paths'
import { deleteVersion, detectInstalledIds, verifyVersion } from '../src/main/versions'

const sha1 = (b: Buffer): string => createHash('sha1').update(b).digest('hex')

// Build a manifest version whose declared hashes match the content we write.
const FABRIC = Buffer.from('fabric-api-content')
const CLIENT = Buffer.from('axoclient-content')

function makeVersion() {
  return {
    id: '1.21.11-r1',
    mcVersion: '1.21.11',
    fabricLoaderVersion: '0.17.3',
    javaMajor: 21,
    client: { version: '0.1.0', url: 'https://x/c.jar', sha1: sha1(CLIENT) },
    mods: [
      {
        id: 'fabric-api',
        source: 'modrinth' as const,
        modrinthProject: 'P',
        modrinthVersion: 'v',
        version: '0.141.5',
        url: 'https://x/f.jar',
        sha1: sha1(FABRIC)
      }
    ]
  }
}

describe('verifyVersion', () => {
  let installDir: string

  beforeEach(async () => {
    installDir = await mkdtemp(join(tmpdir(), 'axo-ver-'))
  })
  afterEach(async () => {
    await rm(installDir, { recursive: true, force: true })
  })

  async function writeInstance(version: ReturnType<typeof makeVersion>): Promise<string> {
    const dir = instanceModsDir(installDir, version.id)
    await mkdir(dir, { recursive: true })
    await writeFile(join(dir, `fabric-api-${version.mods[0].version}.jar`), FABRIC)
    await writeFile(join(dir, `axoclient-${version.client.version}.jar`), CLIENT)
    return dir
  }

  it('reports not-installed for an empty install', async () => {
    const status = await verifyVersion(installDir, makeVersion())
    expect(status.state).toBe('not-installed')
    expect(status.presentFiles).toBe(0)
    expect(status.expectedFiles).toBe(2)
    expect(status.missing).toHaveLength(2)
  })

  it('reports installed when every hash matches', async () => {
    const v = makeVersion()
    await writeInstance(v)
    const status = await verifyVersion(installDir, v)
    expect(status.state).toBe('installed')
    expect(status.presentFiles).toBe(2)
    expect(status.missing).toEqual([])
    expect(status.corrupted).toEqual([])
  })

  it('flags a missing file as partial', async () => {
    const v = makeVersion()
    const dir = await writeInstance(v)
    await rm(join(dir, `axoclient-${v.client.version}.jar`))
    const status = await verifyVersion(installDir, v)
    expect(status.state).toBe('partial')
    expect(status.missing).toEqual([`axoclient-${v.client.version}.jar`])
  })

  it('flags a corrupted file as partial', async () => {
    const v = makeVersion()
    const dir = await writeInstance(v)
    await writeFile(join(dir, `fabric-api-${v.mods[0].version}.jar`), 'tampered')
    const status = await verifyVersion(installDir, v)
    expect(status.state).toBe('partial')
    expect(status.corrupted).toEqual([`fabric-api-${v.mods[0].version}.jar`])
  })
})

describe('detectInstalledIds / deleteVersion', () => {
  let installDir: string

  beforeEach(async () => {
    installDir = await mkdtemp(join(tmpdir(), 'axo-ver2-'))
  })
  afterEach(async () => {
    await rm(installDir, { recursive: true, force: true })
  })

  it('lists instance ids on disk and deletes them', async () => {
    await mkdir(instanceModsDir(installDir, 'a-1'), { recursive: true })
    await mkdir(instanceModsDir(installDir, 'b-2'), { recursive: true })
    expect((await detectInstalledIds(installDir)).sort()).toEqual(['a-1', 'b-2'])

    await deleteVersion(installDir, 'a-1')
    expect((await detectInstalledIds(installDir)).sort()).toEqual(['b-2'])
    await expect(stat(instanceDir(installDir, 'a-1'))).rejects.toThrow()
  })

  it('returns empty when nothing is installed', async () => {
    expect(await detectInstalledIds(installDir)).toEqual([])
  })
})
