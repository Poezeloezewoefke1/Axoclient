import { app } from 'electron'
import { join } from 'node:path'
import { Client } from 'minecraft-launcher-core'
import type { AxoManifest } from './manifest'
import type { AxoSession } from './auth'

/**
 * Game launch via minecraft-launcher-core (MCLC).
 *
 * Scaffold status: wiring only. The full pipeline is Phase 2:
 *  - P2-09: Java 21 provisioning (Adoptium) -> javaPath below
 *  - P2-10: vanilla + Fabric profile installation
 *  - P2-11/P2-12: mod + client jar sync with sha1 verification
 *  - P2-13: progress events into the renderer's Play button
 * MCLC stays isolated behind this module so it is swappable (risk R5).
 */

export type LaunchProgress = (stage: string, detail?: string) => void

type LaunchOptions = Parameters<Client['launch']>[0]

/** Launcher-owned game directory — never the user's .minecraft (decision D-005). */
export function gameRoot(): string {
  return join(app.getPath('appData'), '.axoclient')
}

export async function launchGame(
  manifest: AxoManifest,
  versionId: string,
  session: AxoSession,
  onProgress: LaunchProgress
): Promise<void> {
  const version = Object.values(manifest.channels)
    .flatMap((channel) => channel.versions)
    .find((v) => v.id === versionId)
  if (!version) {
    throw new Error(`Unknown version id: ${versionId}`)
  }

  onProgress('preparing', `Minecraft ${version.mcVersion}, Fabric ${version.fabricLoaderVersion}`)

  // TODO(P2-10): install the Fabric loader profile for
  // version.mcVersion + version.fabricLoaderVersion and pass it via
  // options.version.custom. Until then this launches vanilla only.
  const launcher = new Client()

  const options = {
    // TODO(P2-05/P2-13): type narrows once the msmc -> MCLC handoff is finalized.
    authorization: session.mclcAuth,
    root: gameRoot(),
    version: {
      number: version.mcVersion,
      type: 'release'
    },
    memory: {
      // TODO(P2-04): read from the settings store.
      max: '4G',
      min: '1G'
    }
    // TODO(P2-09): javaPath from the provisioned runtime.
  } as LaunchOptions

  launcher.on('debug', (line: string) => onProgress('debug', line))
  launcher.on('data', (line: string) => onProgress('game', line))
  launcher.on('progress', (progress: { type?: string; task?: number; total?: number }) =>
    onProgress('download', `${progress.type ?? ''} ${progress.task ?? 0}/${progress.total ?? 0}`)
  )

  await launcher.launch(options)
  onProgress('launched')
}
