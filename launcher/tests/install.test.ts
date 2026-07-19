import { describe, expect, it } from 'vitest'
import { desiredModFiles } from '../src/main/install'

const version = {
  id: '1.21.11-r1',
  mcVersion: '1.21.11',
  fabricLoaderVersion: '0.17.3',
  javaMajor: 21,
  client: {
    version: '0.1.0',
    url: 'https://example.com/axoclient-0.1.0.jar',
    sha1: 'a'.repeat(40)
  },
  mods: [
    {
      id: 'sodium',
      source: 'modrinth' as const,
      modrinthProject: 'AANobbMI',
      modrinthVersion: 'v1',
      version: '0.6.0',
      url: 'https://cdn.modrinth.com/sodium.jar',
      sha1: 'b'.repeat(40)
    }
  ]
}

describe('desiredModFiles', () => {
  it('maps mods and the client jar to versioned filenames', () => {
    const files = desiredModFiles(version)
    expect(files).toEqual([
      { fileName: 'sodium-0.6.0.jar', url: 'https://cdn.modrinth.com/sodium.jar', sha1: 'b'.repeat(40) },
      {
        fileName: 'axoclient-0.1.0.jar',
        url: 'https://example.com/axoclient-0.1.0.jar',
        sha1: 'a'.repeat(40)
      }
    ])
  })

  it('changes filenames when versions bump, so stray-removal retires old jars', () => {
    const bumped = { ...version, client: { ...version.client, version: '0.2.0' } }
    const names = desiredModFiles(bumped).map((f) => f.fileName)
    expect(names).toContain('axoclient-0.2.0.jar')
    expect(names).not.toContain('axoclient-0.1.0.jar')
  })
})
