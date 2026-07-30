import type {
  AccountInfo,
  AxoSettings,
  CrashDiagnosis,
  GameProgress,
  LaunchProfile,
  ManifestInfo,
  NewsItem,
  ScreenshotInfo,
  SavedServer,
  SavedSkin,
  SessionInfo,
  SkinInfo,
  UpdateStatus,
  UserMod,
  VersionStatus
} from '../shared/types'

type SyncCounts = { downloaded: number; kept: number; removed: number }
type JvmPreset = { id: string; label: string; description: string; args: string }
type AppliedProfile = { settings: AxoSettings; versionId: string | null }

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
      listServers(): Promise<SavedServer[]>
      saveServer(server: SavedServer): Promise<SavedServer[]>
      removeServer(address: string): Promise<SavedServer[]>
      listProfiles(): Promise<LaunchProfile[]>
      saveProfile(profile: LaunchProfile): Promise<LaunchProfile[]>
      removeProfile(name: string): Promise<LaunchProfile[]>
      applyProfile(name: string): Promise<AppliedProfile>
      launch(versionId: string, joinServer?: string): Promise<void>
      forceClose(): Promise<boolean>
      onGameProgress(callback: (progress: GameProgress) => void): () => void
      installUpdate(): Promise<void>
      openLogs(): Promise<void>
      readLog(): Promise<string>
      getNews(): Promise<NewsItem[]>
      openCommunity(): Promise<void>
      listSkins(): Promise<SavedSkin[]>
      saveSkinFile(name: string, slim: boolean): Promise<SavedSkin[]>
      saveCurrentSkin(name: string): Promise<SavedSkin[]>
      deleteSkin(id: string): Promise<SavedSkin[]>
      wearSkin(id: string, slim: boolean): Promise<string | null>
      listShots(versionId: string): Promise<ScreenshotInfo[]>
      readShot(versionId: string, fileName: string): Promise<string | null>
      deleteShot(versionId: string, fileName: string): Promise<ScreenshotInfo[]>
      revealShot(versionId: string, fileName: string): Promise<void>
      copyShot(versionId: string, fileName: string): Promise<boolean>
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
