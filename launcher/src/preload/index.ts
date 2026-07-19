import { contextBridge, ipcRenderer, type IpcRendererEvent } from 'electron'
import type { AxoSettings, GameProgress, ManifestInfo, SessionInfo } from '../shared/types'

/** The only surface the renderer can call. Keep it small and typed. */
const api = {
  getVersion: (): Promise<string> => ipcRenderer.invoke('app:version'),
  getManifest: (): Promise<ManifestInfo> => ipcRenderer.invoke('manifest:get'),
  getSession: (): Promise<SessionInfo | null> => ipcRenderer.invoke('auth:status'),
  restoreSession: (): Promise<SessionInfo | null> => ipcRenderer.invoke('auth:restore'),
  login: (): Promise<SessionInfo> => ipcRenderer.invoke('auth:login'),
  logout: (): Promise<void> => ipcRenderer.invoke('auth:logout'),
  getSettings: (): Promise<AxoSettings> => ipcRenderer.invoke('settings:get'),
  updateSettings: (patch: Partial<AxoSettings>): Promise<AxoSettings> =>
    ipcRenderer.invoke('settings:update', patch),
  launch: (versionId: string): Promise<void> => ipcRenderer.invoke('game:launch', versionId),
  onGameProgress: (callback: (progress: GameProgress) => void): (() => void) => {
    const listener = (_event: IpcRendererEvent, progress: GameProgress): void =>
      callback(progress)
    ipcRenderer.on('game:progress', listener)
    return () => ipcRenderer.removeListener('game:progress', listener)
  }
}

export type AxoApi = typeof api

contextBridge.exposeInMainWorld('axo', api)
