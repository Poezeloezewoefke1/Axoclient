import type {
  AxoSettings,
  GameProgress,
  ManifestInfo,
  SessionInfo,
  UpdateStatus
} from '../shared/types'

declare global {
  interface Window {
    axo: {
      getVersion(): Promise<string>
      getManifest(): Promise<ManifestInfo>
      getSession(): Promise<SessionInfo | null>
      restoreSession(): Promise<SessionInfo | null>
      login(): Promise<SessionInfo>
      logout(): Promise<void>
      getSettings(): Promise<AxoSettings>
      updateSettings(patch: Partial<AxoSettings>): Promise<AxoSettings>
      launch(versionId: string): Promise<void>
      onGameProgress(callback: (progress: GameProgress) => void): () => void
      installUpdate(): Promise<void>
      openLogs(): Promise<void>
      onUpdateStatus(callback: (status: UpdateStatus) => void): () => void
    }
  }
}

export {}
