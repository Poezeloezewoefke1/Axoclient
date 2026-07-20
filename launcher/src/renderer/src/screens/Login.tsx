import { useState } from 'react'
import logoUrl from '../assets/logo.png'
import type { SessionInfo } from '../../../shared/types'

interface Props {
  onLoggedIn: (session: SessionInfo) => void
}

export default function LoginScreen({ onLoggedIn }: Props): React.JSX.Element {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const login = (): void => {
    setBusy(true)
    setError(null)
    window.axo
      .login()
      .then(onLoggedIn)
      .catch((e: unknown) => {
        // P2-08 replaces raw messages with a mapped, human-readable error matrix.
        setError(e instanceof Error ? e.message : 'Sign-in failed. Please try again.')
      })
      .finally(() => setBusy(false))
  }

  return (
    <div className="login-screen">
      <div className="login-card">
        <img src={logoUrl} alt="" className="login-mark" />
        <div className="logo login-logo">
          <span className="logo-axo">AXO</span>
          <span className="logo-sub">CLIENT</span>
        </div>
        <p className="login-tagline">Sign in with your Microsoft account to play.</p>
        <button className="primary-button" onClick={login} disabled={busy}>
          {busy ? 'Waiting for Microsoft…' : 'Sign in with Microsoft'}
        </button>
        {error && <p className="error-text">{error}</p>}
        <p className="login-footnote">
          Axo Client is not affiliated with Mojang or Microsoft. A Minecraft: Java Edition
          license is required.
        </p>
      </div>
    </div>
  )
}
