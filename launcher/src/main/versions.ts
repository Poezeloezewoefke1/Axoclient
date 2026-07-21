import { readdir, rm } from 'node:fs/promises'
import { join } from 'node:path'
import { sha1File } from './download'
import { desiredModFiles } from './install'
import { instanceDir, instanceModsDir, instancesRoot } from './paths'
import type { AxoManifest } from './manifest'
import type { VersionState, VersionStatus } from '../shared/types'

/**
 * Version registry (multi-version launcher management): detect, verify, and
 * delete installed Axo versions. All filesystem-based and unit-tested;
 * the launch/install pipeline shares the same instance layout via paths.ts.
 */

type ManifestVersion = AxoManifest['channels'][string]['versions'][number]

export type { VersionState, VersionStatus }

/** Verify one version's mods folder against the manifest's hashes. */
export async function verifyVersion(
  installDir: string,
  version: ManifestVersion,
  channel = 'stable'
): Promise<VersionStatus> {
  const modsDir = instanceModsDir(installDir, version.id)
  const desired = desiredModFiles(version)

  let present = new Set<string>()
  try {
    present = new Set(await readdir(modsDir))
  } catch {
    // instance/mods dir does not exist yet
  }

  const missing: string[] = []
  const corrupted: string[] = []
  for (const file of desired) {
    if (!present.has(file.fileName)) {
      missing.push(file.fileName)
      continue
    }
    const actual = await sha1File(join(modsDir, file.fileName))
    if (actual !== file.sha1) {
      corrupted.push(file.fileName)
    }
  }

  const ok = desired.length - missing.length - corrupted.length
  let state: VersionState
  if (ok === 0) {
    state = 'not-installed'
  } else if (missing.length > 0 || corrupted.length > 0) {
    state = 'partial'
  } else {
    state = 'installed'
  }

  return {
    id: version.id,
    mcVersion: version.mcVersion,
    channel,
    state,
    presentFiles: ok,
    expectedFiles: desired.length,
    missing,
    corrupted
  }
}

/** Status of every version the manifest offers, across all channels. */
export async function listVersions(
  installDir: string,
  manifest: AxoManifest
): Promise<VersionStatus[]> {
  const out: VersionStatus[] = []
  for (const [channel, data] of Object.entries(manifest.channels)) {
    for (const version of data.versions) {
      out.push(await verifyVersion(installDir, version, channel))
    }
  }
  return out
}

/** Instance directory ids actually present on disk (may include partials). */
export async function detectInstalledIds(installDir: string): Promise<string[]> {
  try {
    return await readdir(instancesRoot(installDir))
  } catch {
    return []
  }
}

/** Remove a version's entire instance directory (mods, saves, everything). */
export async function deleteVersion(installDir: string, versionId: string): Promise<void> {
  await rm(instanceDir(installDir, versionId), { recursive: true, force: true })
}
