import { app } from 'electron'
import { readFile, writeFile, mkdir } from 'node:fs/promises'
import { join } from 'node:path'
import { z } from 'zod'
import { compareSemver } from './semver'
import type { ManifestInfo } from '../shared/types'

/**
 * Fetch + validate axo-manifest.json (spec: docs/manifest-spec.md).
 * Falls back to the last cached valid manifest when the network or
 * validation fails, flagging the result as stale.
 *
 * TODO(P3-07): point at the website URL once deployed
 * (https://<site-domain>/manifest/axo-manifest.json).
 */
const MANIFEST_URL =
  'https://raw.githubusercontent.com/Poezeloezewoefke1/Claud/main/manifest/axo-manifest.json'

const FETCH_TIMEOUT_MS = 10_000

const artifactSchema = z.object({
  version: z.string(),
  url: z.string().url(),
  sha1: z.string().regex(/^[0-9a-f]{40}$/),
  size: z.number().int().optional()
})

const modSchema = artifactSchema.extend({
  id: z.string(),
  source: z.literal('modrinth'),
  modrinthProject: z.string(),
  modrinthVersion: z.string(),
  required: z.boolean().optional()
})

const versionSchema = z.object({
  id: z.string(),
  mcVersion: z.string(),
  fabricLoaderVersion: z.string(),
  javaMajor: z.number().int(),
  notes: z.string().optional(),
  client: artifactSchema,
  mods: z.array(modSchema)
})

const channelSchema = z.object({
  default: z.string(),
  versions: z.array(versionSchema)
})

// passthrough(): schemaVersion 1 changes are additive; unknown fields must not break old launchers.
export const manifestSchema = z
  .object({
    schemaVersion: z.literal(1),
    generatedAt: z.string(),
    launcher: z.object({
      minimumVersion: z.string(),
      releasesRepo: z.string()
    }),
    channels: z.record(channelSchema)
  })
  .passthrough()

export type AxoManifest = z.infer<typeof manifestSchema>

function cachePath(): string {
  return join(app.getPath('userData'), 'manifest.cache.json')
}

async function fetchRemote(): Promise<AxoManifest> {
  const response = await fetch(MANIFEST_URL, {
    signal: AbortSignal.timeout(FETCH_TIMEOUT_MS)
  })
  if (!response.ok) {
    throw new Error(`Manifest fetch failed: HTTP ${response.status}`)
  }
  return manifestSchema.parse(await response.json())
}

async function readCache(): Promise<AxoManifest | null> {
  try {
    return manifestSchema.parse(JSON.parse(await readFile(cachePath(), 'utf8')))
  } catch {
    return null
  }
}

async function writeCache(manifest: AxoManifest): Promise<void> {
  await mkdir(app.getPath('userData'), { recursive: true })
  await writeFile(cachePath(), JSON.stringify(manifest), 'utf8')
}

/** Full manifest for the install/launch pipeline (main process only). */
export async function getManifest(): Promise<{ manifest: AxoManifest; stale: boolean }> {
  try {
    const manifest = await fetchRemote()
    await writeCache(manifest).catch(() => undefined)
    return { manifest, stale: false }
  } catch (error) {
    const cached = await readCache()
    if (cached) {
      return { manifest: cached, stale: true }
    }
    throw error
  }
}

/** Renderer-safe projection sent over IPC. */
export async function getManifestInfo(): Promise<ManifestInfo> {
  const { manifest, stale } = await getManifest()
  // Forced-update gate (P3-03): launchers older than minimumVersion must not install/launch.
  const forcedUpdate = compareSemver(app.getVersion(), manifest.launcher.minimumVersion) < 0
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
  return { stale, forcedUpdate, channels }
}
