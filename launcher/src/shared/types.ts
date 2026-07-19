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
  channels: Record<string, ChannelInfo>
}

export interface SessionInfo {
  username: string
  uuid: string
}
