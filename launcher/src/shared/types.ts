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

export interface AccountInfo {
  username: string
  uuid: string
  /** True for the account currently signed in and used to launch. */
  active: boolean
}

export interface SkinInfo {
  /** data:image/png;base64,… of the 64×64 skin texture, or null if none. */
  dataUrl: string | null
  /** True for the 3px-arm "slim"/Alex model. */
  slim: boolean
}

export interface UpdateStatus {
  state: 'ready'
  version: string
}

export type VersionState = 'installed' | 'partial' | 'not-installed'

export interface VersionStatus {
  id: string
  mcVersion: string
  channel: string
  state: VersionState
  presentFiles: number
  expectedFiles: number
  missing: string[]
  corrupted: string[]
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
  /** Transfer stats, present while a file is downloading (mods/java/game). */
  received?: number
  total?: number
  bytesPerSecond?: number
  etaSeconds?: number | null
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
  /** True once the first-run onboarding has been completed (P5-05). */
  onboarded: boolean
}
