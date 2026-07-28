import type {
  AccountInfo,
  AxoSettings,
  CrashDiagnosis,
  GameProgress,
  ManifestInfo,
  NewsItem,
  SavedSkin,
  SessionInfo,
  SkinInfo,
  UpdateStatus,
  UserMod,
  VersionStatus
} from '../shared/types'

type SyncCounts = { downloaded: number; kept: number; removed: number }
type JvmPreset = { id: string; label: string; description: string; args: string }

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
      readLog(): Promise<string>
      getNews(): Promise<NewsItem[]>
      listSkins(): Promise<SavedSkin[]>
      saveSkinFile(name: string, slim: boolean): Promise<SavedSkin[]>
      saveCurrentSkin(name: string): Promise<SavedSkin[]>
      deleteSkin(id: string): Promise<SavedSkin[]>
      wearSkin(id: string, slim: boolean): Promise<string | null>
      listMods(versionId: string): Promise<UserMod[]>
      addMods(versionId: string): Promise<UserMod[]>
      setModEnabled(versionId: string, fileName: string, enabled: boolean): Promise<UserMod[]>
      removeMod(versionId: string, fileName: string): Promise<UserMod[]>
      getCrashReport(versionId: string): Promise<CrashDiagnosis | null>
      getRecommendedRam(): Promise<number>
      getJvmPresets(): Promise<JvmPreset[]>
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
