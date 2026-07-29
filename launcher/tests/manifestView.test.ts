import { describe, expect, it } from 'vitest'
import { projectChannels, resolveDefaultVersion } from '../src/main/manifestView'
import type { AxoManifest } from '../src/main/manifest'

/**
 * These tests exist to hold milestone M4 honest: "adding a Minecraft version
 * is a data change, not a code change". Nothing in the projection knows a
 * version number, so a two-version, two-channel manifest must flow straight
 * through to the picker with no launcher edits.
 */

function artifact(name: string): AxoManifest['channels'][string]['versions'][number]['client'] {
  return {
    version: '0.1.0',
    url: `https://example.invalid/${name}.jar`,
    sha1: 'a'.repeat(40),
    size: 1234
  }
}

function version(
  id: string,
  mcVersion: string,
  notes?: string
): AxoManifest['channels'][string]['versions'][number] {
  return {
    id,
    mcVersion,
    fabricLoaderVersion: '0.17.3',
    javaMajor: 21,
    notes,
    client: artifact(id),
    mods: []
  }
}

const twoVersions: AxoManifest = {
  schemaVersion: 1,
  generatedAt: '2026-07-29T00:00:00Z',
  launcher: { minimumVersion: '0.1.0', releasesRepo: 'owner/repo' },
  channels: {
    stable: {
      default: '1.21.11-r1',
      versions: [version('1.21.11-r1', '1.21.11', 'First release'), version('1.21.9-r1', '1.21.9')]
    },
    beta: {
      default: '1.22-r1',
      versions: [version('1.22-r1', '1.22')]
    }
  }
} as AxoManifest

describe('projectChannels', () => {
  it('carries every channel and version through', () => {
    const channels = projectChannels(twoVersions)
    expect(Object.keys(channels).sort()).toEqual(['beta', 'stable'])
    expect(channels.stable.versions.map((v) => v.id)).toEqual(['1.21.11-r1', '1.21.9-r1'])
    expect(channels.beta.versions.map((v) => v.mcVersion)).toEqual(['1.22'])
  })

  it('keeps each channel its own default', () => {
    const channels = projectChannels(twoVersions)
    expect(channels.stable.default).toBe('1.21.11-r1')
    expect(channels.beta.default).toBe('1.22-r1')
  })

  it('preserves notes and drops them when absent', () => {
    const channels = projectChannels(twoVersions)
    expect(channels.stable.versions[0].notes).toBe('First release')
    expect(channels.stable.versions[1].notes).toBeUndefined()
  })

  it('never leaks urls, hashes or mod lists to the renderer', () => {
    const serialised = JSON.stringify(projectChannels(twoVersions))
    expect(serialised).not.toContain('example.invalid')
    expect(serialised).not.toContain('a'.repeat(40))
    expect(serialised).not.toContain('mods')
  })

  it('handles an empty channel map without throwing', () => {
    expect(projectChannels({ ...twoVersions, channels: {} } as AxoManifest)).toEqual({})
  })
})

describe('resolveDefaultVersion', () => {
  const channels = projectChannels(twoVersions)

  it('returns the channel default', () => {
    expect(resolveDefaultVersion(channels, 'stable')).toBe('1.21.11-r1')
    expect(resolveDefaultVersion(channels, 'beta')).toBe('1.22-r1')
  })

  it('falls back to the first channel for an unknown channel name', () => {
    expect(resolveDefaultVersion(channels, 'nightly')).toBe('1.21.11-r1')
  })

  it('falls back to the first version when default names something missing', () => {
    const broken = projectChannels({
      ...twoVersions,
      channels: { stable: { default: 'typo', versions: [version('1.21.11-r1', '1.21.11')] } }
    } as AxoManifest)
    expect(resolveDefaultVersion(broken, 'stable')).toBe('1.21.11-r1')
  })

  it('returns null when there is nothing installable', () => {
    expect(resolveDefaultVersion({}, 'stable')).toBeNull()
    expect(
      resolveDefaultVersion({ stable: { default: 'x', versions: [] } }, 'stable')
    ).toBeNull()
  })
})
