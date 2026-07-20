import { join } from 'node:path'
import { Client } from 'minecraft-launcher-core'
import type { AxoManifest } from './manifest'
import type { AxoSession } from './auth'
import type { AxoSettings, GameProgress } from '../shared/types'
import { ensureJava } from './java'
import { installFabricProfile } from './fabricProfile'
import { syncModsFolder } from './install'
import { createGameLog, logLine } from './logger'

/**
 * The full launch pipeline (roadmap P2-13): Java → Fabric profile →
 * mod sync → minecraft-launcher-core. Every stage is idempotent, so
 * this doubles as the client-files update check (P3-04) — a manifest
 * bump simply changes what the sync stage downloads. MCLC stays
 * isolated behind this module (risk R5). Resolves when the game exits.
 */

type LaunchOptions = Parameters<Client['launch']>[0]

let currentProcess: { kill: (signal?: NodeJS.Signals | number) => boolean } | null = null

/** Force-close the running game (crash-handling.md case 6). Returns false when nothing runs. */
export function forceCloseGame(): boolean {
  if (!currentProcess) {
    return false
  }
  logLine('launch', 'force-closing game process')
  return currentProcess.kill('SIGKILL')
}

export async function launchGame(
  manifest: AxoManifest,
  versionId: string,
  session: AxoSession,
  settings: AxoSettings,
  onProgress: (progress: GameProgress) => void
): Promise<void> {
  const version = Object.values(manifest.channels)
    .flatMap((channel) => channel.versions)
    .find((v) => v.id === versionId)
  if (!version) {
    throw new Error(`Unknown version id: ${versionId}`)
  }

  onProgress({ stage: 'preparing', detail: `Minecraft ${version.mcVersion}` })
  logLine('launch', `pipeline start: ${version.id} for ${session.username}`)

  const javaPath = await ensureJava(version.javaMajor, join(settings.installDir, 'runtime'), {
    onProgress: (stage, detail) => onProgress({ stage: 'java', detail: detail ?? stage })
  })

  const profileId = await installFabricProfile(
    settings.installDir,
    version.mcVersion,
    version.fabricLoaderVersion
  )

  onProgress({ stage: 'mods' })
  await syncModsFolder(settings.installDir, version, (event) =>
    onProgress({ stage: 'mods', detail: `${event.action} ${event.file}` })
  )

  onProgress({ stage: 'launching' })
  const launcher = new Client()
  launcher.on('progress', (progress: { type?: string; task?: number; total?: number }) =>
    onProgress({
      stage: 'downloading',
      detail: `${progress.type ?? ''} ${progress.task ?? 0}/${progress.total ?? 0}`.trim()
    })
  )
  const gameLog = createGameLog()
  launcher.on('debug', (line: string) => logLine('mclc', line))
  launcher.on('data', (line: string) => gameLog.append(String(line).trimEnd()))

  const options = {
    authorization: session.mclcAuth,
    root: settings.installDir,
    javaPath,
    version: {
      number: version.mcVersion,
      type: 'release',
      custom: profileId
    },
    memory: {
      max: `${settings.ramMb}M`,
      min: '1024M'
    },
    customArgs: settings.jvmArgs ? settings.jvmArgs.split(/\s+/).filter(Boolean) : undefined
  } as LaunchOptions

  const process = await launcher.launch(options)
  if (!process) {
    throw new Error('Game process failed to start — see launcher.log')
  }
  currentProcess = process
  onProgress({ stage: 'running' })

  await new Promise<void>((resolve) => {
    process.on('close', (code: number | null) => {
      gameLog.append(`--- exited with code ${code} ---`)
      logLine('launch', `game exited with code ${code}`)
      currentProcess = null
      resolve()
    })
  })
  onProgress({ stage: 'closed' })
}
