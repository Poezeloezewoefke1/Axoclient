import { join } from 'node:path'

/**
 * Per-version instance layout (multi-version support). Vanilla files
 * (libraries, assets, version JSONs) stay shared under the install root to
 * avoid duplication; each Axo version gets its own instance directory that
 * becomes the game's working dir, so its mods and saves never mix with
 * another version's.
 *
 *   <installDir>/                 shared root (MCLC root)
 *     libraries/ assets/ versions/ runtime/
 *     instances/<versionId>/      per-version game directory
 *       mods/                     that version's Axo jar + bundled mods
 *       saves/ ...                created by the game
 */
export function instanceDir(installDir: string, versionId: string): string {
  return join(installDir, 'instances', versionId)
}

export function instanceModsDir(installDir: string, versionId: string): string {
  return join(instanceDir(installDir, versionId), 'mods')
}

export function instancesRoot(installDir: string): string {
  return join(installDir, 'instances')
}
