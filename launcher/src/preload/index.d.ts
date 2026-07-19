import type { ManifestInfo, SessionInfo } from '../shared/types'

declare global {
  interface Window {
    axo: {
      getVersion(): Promise<string>
      getManifest(): Promise<ManifestInfo>
      getSession(): Promise<SessionInfo | null>
      login(): Promise<SessionInfo>
      logout(): Promise<void>
    }
  }
}

export {}
