import { contextBridge, ipcRenderer, type IpcRendererEvent } from 'electron'
import type {
  AccountInfo,
  AxoSettings,
  CrashDiagnosis,
  GameProgress,
  LaunchProfile,
  ManifestInfo,
  NewsItem,
  ScreenshotInfo,
  SavedSkin,
  SessionInfo,
  SkinInfo,
  UpdateStatus,
  UserMod,
  VersionStatus
} from '../shared/types'

type SyncCounts = { downloaded: number; kept: number; removed: number }
type AppliedProfile = { settings: AxoSettings; versionId: string | null }
type JvmPreset = { id: string; label: string; description: string; args: string }

/** The only surface the renderer can call. Keep it small and typed. */
const api = {
  getVersion: (): Promise<string> => ipcRenderer.invoke('app:version'),
  getManifest: (): Promise<ManifestInfo> => ipcRenderer.invoke('manifest:get'),
  getSession: (): Promise<SessionInfo | null> => ipcRenderer.invoke('auth:status'),
  restoreSession: (): Promise<SessionInfo | null> => ipcRenderer.invoke('auth:restore'),
  login: (): Promise<SessionInfo> => ipcRenderer.invoke('auth:login'),
  logout: (): Promise<SessionInfo | null> => ipcRenderer.invoke('auth:logout'),
  listAccounts: (): Promise<AccountInfo[]> => ipcRenderer.invoke('accounts:list'),
  selectAccount: (uuid: string): Promise<SessionInfo | null> =>
    ipcRenderer.invoke('accounts:select', uuid),
  removeAccount: (uuid: string): Promise<SessionInfo | null> =>
    ipcRenderer.invoke('accounts:remove', uuid),
  getSettings: (): Promise<AxoSettings> => ipcRenderer.invoke('settings:get'),
  updateSettings: (patch: Partial<AxoSettings>): Promise<AxoSettings> =>
    ipcRenderer.invoke('settings:update', patch),
  listProfiles: (): Promise<LaunchProfile[]> => ipcRenderer.invoke('profiles:list'),
  saveProfile: (profile: LaunchProfile): Promise<LaunchProfile[]> =>
    ipcRenderer.invoke('profiles:save', profile),
  removeProfile: (name: string): Promise<LaunchProfile[]> =>
    ipcRenderer.invoke('profiles:remove', name),
  applyProfile: (name: string): Promise<AppliedProfile> =>
    ipcRenderer.invoke('profiles:apply', name),
  launch: (versionId: string, joinServer?: string): Promise<void> =>
    ipcRenderer.invoke('game:launch', versionId, joinServer),
  forceClose: (): Promise<boolean> => ipcRenderer.invoke('game:forceClose'),
  onGameProgress: (callback: (progress: GameProgress) => void): (() => void) => {
    const listener = (_event: IpcRendererEvent, progress: GameProgress): void =>
      callback(progress)
    ipcRenderer.on('game:progress', listener)
    return () => ipcRenderer.removeListener('game:progress', listener)
  },
  installUpdate: (): Promise<void> => ipcRenderer.invoke('update:install'),
  openLogs: (): Promise<void> => ipcRenderer.invoke('logs:open'),
  readLog: (): Promise<string> => ipcRenderer.invoke('logs:read'),
  getNews: (): Promise<NewsItem[]> => ipcRenderer.invoke('news:get'),
  listSkins: (): Promise<SavedSkin[]> => ipcRenderer.invoke('skins:list'),
  saveSkinFile: (name: string, slim: boolean): Promise<SavedSkin[]> =>
    ipcRenderer.invoke('skins:saveFile', name, slim),
  saveCurrentSkin: (name: string): Promise<SavedSkin[]> =>
    ipcRenderer.invoke('skins:saveCurrent', name),
  deleteSkin: (id: string): Promise<SavedSkin[]> => ipcRenderer.invoke('skins:delete', id),
  wearSkin: (id: string, slim: boolean): Promise<string | null> =>
    ipcRenderer.invoke('skins:wear', id, slim),
  listShots: (versionId: string): Promise<ScreenshotInfo[]> =>
    ipcRenderer.invoke('shots:list', versionId),
  readShot: (versionId: string, fileName: string): Promise<string | null> =>
    ipcRenderer.invoke('shots:read', versionId, fileName),
  deleteShot: (versionId: string, fileName: string): Promise<ScreenshotInfo[]> =>
    ipcRenderer.invoke('shots:delete', versionId, fileName),
  revealShot: (versionId: string, fileName: string): Promise<void> =>
    ipcRenderer.invoke('shots:reveal', versionId, fileName),
  copyShot: (versionId: string, fileName: string): Promise<boolean> =>
    ipcRenderer.invoke('shots:copy', versionId, fileName),
  listMods: (versionId: string): Promise<UserMod[]> => ipcRenderer.invoke('mods:list', versionId),
  addMods: (versionId: string): Promise<UserMod[]> => ipcRenderer.invoke('mods:add', versionId),
  setModEnabled: (versionId: string, fileName: string, enabled: boolean): Promise<UserMod[]> =>
    ipcRenderer.invoke('mods:setEnabled', versionId, fileName, enabled),
  removeMod: (versionId: string, fileName: string): Promise<UserMod[]> =>
    ipcRenderer.invoke('mods:remove', versionId, fileName),
  getCrashReport: (versionId: string): Promise<CrashDiagnosis | null> =>
    ipcRenderer.invoke('crash:latest', versionId),
  getRecommendedRam: (): Promise<number> => ipcRenderer.invoke('system:recommendedRam'),
  getJvmPresets: (): Promise<JvmPreset[]> => ipcRenderer.invoke('system:jvmPresets'),
  getSkin: (): Promise<SkinInfo> => ipcRenderer.invoke('skin:get'),
  applySkin: (variant: 'classic' | 'slim'): Promise<string | null> =>
    ipcRenderer.invoke('skin:apply', variant),
  repair: (): Promise<SyncCounts> => ipcRenderer.invoke('game:repair'),
  listVersions: (): Promise<VersionStatus[]> => ipcRenderer.invoke('versions:list'),
  installVersion: (versionId: string): Promise<SyncCounts> =>
    ipcRenderer.invoke('versions:install', versionId),
  deleteVersion: (versionId: string): Promise<void> =>
    ipcRenderer.invoke('versions:delete', versionId),
  onUpdateStatus: (callback: (status: UpdateStatus) => void): (() => void) => {
    const listener = (_event: IpcRendererEvent, status: UpdateStatus): void => callback(status)
    ipcRenderer.on('update:status', listener)
    return () => ipcRenderer.removeListener('update:status', listener)
  }
}

export type AxoApi = typeof api

contextBridge.exposeInMainWorld('axo', api)
