import { mkdir, stat, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { z } from 'zod'
import { fetchJson } from './download'

/**
 * Installs the Fabric loader launch profile (roadmap P2-10): fetch the
 * version JSON from Fabric's meta server and place it under
 * versions/<id>/<id>.json where minecraft-launcher-core picks it up via
 * options.version.custom. Idempotent — an existing profile is reused
 * without a network call.
 */

const profileSchema = z.object({ id: z.string().min(1) }).passthrough()

export function fabricProfileUrl(mcVersion: string, loaderVersion: string): string {
  return `https://meta.fabricmc.net/v2/versions/loader/${mcVersion}/${loaderVersion}/profile/json`
}

/** The id Fabric's meta server assigns to a profile. */
export function fabricProfileId(mcVersion: string, loaderVersion: string): string {
  return `fabric-loader-${loaderVersion}-${mcVersion}`
}

export async function installFabricProfile(
  root: string,
  mcVersion: string,
  loaderVersion: string,
  fetchJsonImpl: (url: string) => Promise<unknown> = fetchJson
): Promise<string> {
  const expectedId = fabricProfileId(mcVersion, loaderVersion)
  const expectedFile = join(root, 'versions', expectedId, `${expectedId}.json`)
  try {
    await stat(expectedFile)
    return expectedId
  } catch {
    // Not installed yet.
  }

  const raw = await fetchJsonImpl(fabricProfileUrl(mcVersion, loaderVersion))
  const profile = profileSchema.parse(raw)
  const dir = join(root, 'versions', profile.id)
  await mkdir(dir, { recursive: true })
  await writeFile(join(dir, `${profile.id}.json`), JSON.stringify(raw), 'utf8')
  return profile.id
}
