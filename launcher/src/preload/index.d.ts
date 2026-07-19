import type { AxoSettings, ManifestInfo, SessionInfo } from '../shared/types'

declare global {
  interface Window {
    axo: {
      getVersion(): Promise<string>
      getManifest(): Promise<ManifestInfo>
      getSession(): Promise<SessionInfo | null>
      login(): Promise<SessionInfo>
      logout(): Promise<void>
      getSettings(): Promise<AxoSettings>
      updateSettings(patch: Partial<AxoSettings>): Promise<AxoSettings>
    }
  }
}

export {}
