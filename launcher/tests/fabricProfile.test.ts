import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { fabricProfileId, fabricProfileUrl, installFabricProfile } from '../src/main/fabricProfile'

const PROFILE = {
  id: 'fabric-loader-0.17.3-1.21.11',
  inheritsFrom: '1.21.11',
  libraries: []
}

describe('fabricProfileUrl / fabricProfileId', () => {
  it('builds the meta URL and profile id', () => {
    expect(fabricProfileUrl('1.21.11', '0.17.3')).toBe(
      'https://meta.fabricmc.net/v2/versions/loader/1.21.11/0.17.3/profile/json'
    )
    expect(fabricProfileId('1.21.11', '0.17.3')).toBe('fabric-loader-0.17.3-1.21.11')
  })
})

describe('installFabricProfile', () => {
  let root: string

  beforeEach(async () => {
    root = await mkdtemp(join(tmpdir(), 'axo-fabric-'))
  })

  afterEach(async () => {
    await rm(root, { recursive: true, force: true })
  })

  it('writes the profile under versions/<id>/<id>.json and returns the id', async () => {
    const id = await installFabricProfile(root, '1.21.11', '0.17.3', async () => PROFILE)
    expect(id).toBe('fabric-loader-0.17.3-1.21.11')
    const onDisk = JSON.parse(await readFile(join(root, 'versions', id, `${id}.json`), 'utf8'))
    expect(onDisk).toEqual(PROFILE)
  })

  it('is idempotent — the second call needs no network', async () => {
    await installFabricProfile(root, '1.21.11', '0.17.3', async () => PROFILE)
    const id = await installFabricProfile(root, '1.21.11', '0.17.3', async () => {
      throw new Error('network must not be used when the profile exists')
    })
    expect(id).toBe('fabric-loader-0.17.3-1.21.11')
  })

  it('rejects malformed profile responses', async () => {
    await expect(
      installFabricProfile(root, '1.21.11', '0.17.3', async () => ({ nope: true }))
    ).rejects.toThrow()
  })
})
