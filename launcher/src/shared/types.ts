/**
 * Types crossing the IPC boundary. Renderer sees only these — tokens and
 * auth objects never leave the main process.
 */

export interface VersionInfo {
  id: string
  mcVersion: string
  notes?: string
}

export interface ChannelInfo {
  default: string
  versions: VersionInfo[]
}

export interface ManifestInfo {
  /** True when served from the on-disk cache because fetch/validation failed. */
  stale: boolean
  /** True when this launcher is older than the manifest's launcher.minimumVersion. */
  forcedUpdate: boolean
  channels: Record<string, ChannelInfo>
}

export interface SessionInfo {
  username: string
  uuid: string
}

export interface UpdateStatus {
  state: 'ready'
  version: string
}

export type GameStage =
  | 'preparing'
  | 'java'
  | 'mods'
  | 'downloading'
  | 'launching'
  | 'running'
  | 'closed'

export interface GameProgress {
  stage: GameStage
  detail?: string
}

export interface AxoSettings {
  /** Maximum game memory in MiB. Clamped to SETTINGS_LIMITS in the store. */
  ramMb: number
  /** Manifest channel to install from. */
  channel: string
  /** Game install directory (launcher-owned, decision D-005). */
  installDir: string
  /** Extra JVM arguments, space-separated. Empty = none. */
  jvmArgs: string
}
