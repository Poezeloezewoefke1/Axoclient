import type { AxoManifest } from './manifest'
import { instanceModsDir } from './paths'
import { protectedModFiles } from './userMods'
import {
  syncDirectory,
  type DesiredFile,
  type SyncEvent,
  type SyncProgress,
  type SyncResult
} from './sync'

/**
 * Install-pipeline pieces that are pure manifest→filesystem mapping.
 * The mods folder (bundled mods + the Axo client jar, P2-11/P2-12) is
 * fully implemented; vanilla + Fabric profile installation via MCLC is
 * P2-10 and lives in launch.ts once wired.
 */

type ManifestVersion = AxoManifest['channels'][string]['versions'][number]

/** Versioned filenames so a manifest bump changes the name → stray-removal retires the old jar. */
export function desiredModFiles(version: ManifestVersion): DesiredFile[] {
  const files: DesiredFile[] = version.mods.map((mod) => ({
    fileName: `${mod.id}-${mod.version}.jar`,
    url: mod.url,
    sha1: mod.sha1
  }))
  files.push({
    fileName: `axoclient-${version.client.version}.jar`,
    url: version.client.url,
    sha1: version.client.sha1
  })
  return files
}

/**
 * Sync a version's mods into its own instance folder (per-version isolation).
 * Mods the player added themselves are passed through as protected, so the
 * stray-removal pass leaves them alone.
 */
export async function syncModsFolder(
  installDir: string,
  version: ManifestVersion,
  onEvent?: (event: SyncEvent) => void,
  onProgress?: (progress: SyncProgress) => void
): Promise<SyncResult> {
  return syncDirectory(
    instanceModsDir(installDir, version.id),
    desiredModFiles(version),
    onEvent,
    onProgress,
    await protectedModFiles(installDir, version.id)
  )
}
