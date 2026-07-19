import { contextBridge, ipcRenderer } from 'electron'
import type { AxoSettings, ManifestInfo, SessionInfo } from '../shared/types'

/** The only surface the renderer can call. Keep it small and typed. */
const api = {
  getVersion: (): Promise<string> => ipcRenderer.invoke('app:version'),
  getManifest: (): Promise<ManifestInfo> => ipcRenderer.invoke('manifest:get'),
  getSession: (): Promise<SessionInfo | null> => ipcRenderer.invoke('auth:status'),
  login: (): Promise<SessionInfo> => ipcRenderer.invoke('auth:login'),
  logout: (): Promise<void> => ipcRenderer.invoke('auth:logout'),
  getSettings: (): Promise<AxoSettings> => ipcRenderer.invoke('settings:get'),
  updateSettings: (patch: Partial<AxoSettings>): Promise<AxoSettings> =>
    ipcRenderer.invoke('settings:update', patch)
}

export type AxoApi = typeof api

contextBridge.exposeInMainWorld('axo', api)
