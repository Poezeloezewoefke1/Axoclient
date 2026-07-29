import type { ManifestInfo } from '../shared/types'
import type { AxoManifest } from './manifest'

/**
 * The manifest → renderer projection, split out of manifest.ts so it can be
 * unit-tested: manifest.ts imports electron's `app` for the version check,
 * which makes it unloadable outside a browser.
 *
 * This is the code path that makes "adding a Minecraft version is a data
 * change" true (milestone M4) — nothing here knows what 1.21.11 is, so a new
 * manifest entry flows to the version picker on its own.
 */

/** Renderer-safe channel/version list. Strips URLs, hashes and mod lists. */
export function projectChannels(manifest: AxoManifest): ManifestInfo['channels'] {
  const channels: ManifestInfo['channels'] = {}
  for (const [name, channel] of Object.entries(manifest.channels)) {
    channels[name] = {
      default: channel.default,
      versions: channel.versions.map((v) => ({
        id: v.id,
        mcVersion: v.mcVersion,
        notes: v.notes
      }))
    }
  }
  return channels
}

/**
 * Pick the version a channel should install.
 *
 * Falls back to the first listed version when `default` names something that
 * isn't in the list — a typo in the manifest should degrade to "install
 * something reasonable", not to a launcher that refuses to launch.
 */
export function resolveDefaultVersion(
  channels: ManifestInfo['channels'],
  channelName: string
): string | null {
  const channel = channels[channelName] ?? Object.values(channels)[0]
  if (!channel || channel.versions.length === 0) {
    return null
  }
  const named = channel.versions.find((v) => v.id === channel.default)
  return named ? named.id : channel.versions[0].id
}
