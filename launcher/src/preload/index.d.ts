import type {
  AccountInfo,
  AxoSettings,
  GameProgress,
  ManifestInfo,
  SessionInfo,
  SkinInfo,
  UpdateStatus,
  VersionStatus
} from '../shared/types'

type SyncCounts = { downloaded: number; kept: number; removed: number }

declare global {
  interface Window {
    axo: {
      getVersion(): Promise<string>
      getManifest(): Promise<ManifestInfo>
      getSession(): Promise<SessionInfo | null>
      restoreSession(): Promise<SessionInfo | null>
      login(): Promise<SessionInfo>
      logout(): Promise<SessionInfo | null>
      listAccounts(): Promise<AccountInfo[]>
      selectAccount(uuid: string): Promise<SessionInfo | null>
      removeAccount(uuid: string): Promise<SessionInfo | null>
      getSettings(): Promise<AxoSettings>
      updateSettings(patch: Partial<AxoSettings>): Promise<AxoSettings>
      launch(versionId: string): Promise<void>
      forceClose(): Promise<boolean>
      onGameProgress(callback: (progress: GameProgress) => void): () => void
      installUpdate(): Promise<void>
      openLogs(): Promise<void>
      getSkin(): Promise<SkinInfo>
      applySkin(variant: 'classic' | 'slim'): Promise<string | null>
      repair(): Promise<SyncCounts>
      listVersions(): Promise<VersionStatus[]>
      installVersion(versionId: string): Promise<SyncCounts>
      deleteVersion(versionId: string): Promise<void>
      onUpdateStatus(callback: (status: UpdateStatus) => void): () => void
    }
  }
}

export {}
