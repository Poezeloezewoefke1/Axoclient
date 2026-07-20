import { useEffect, useState } from 'react'
import logoUrl from './assets/logo.png'
import HomeScreen from './screens/Home'
import LoginScreen from './screens/Login'
import SettingsScreen from './screens/Settings'
import type { AxoSettings, SessionInfo, UpdateStatus } from '../../shared/types'

type Screen = 'home' | 'settings'

export default function App(): React.JSX.Element {
  const [screen, setScreen] = useState<Screen>('home')
  const [session, setSession] = useState<SessionInfo | null>(null)
  const [restoring, setRestoring] = useState(true)
  const [version, setVersion] = useState('')
  const [update, setUpdate] = useState<UpdateStatus | null>(null)
  const [onboarding, setOnboarding] = useState<AxoSettings | null>(null)

  useEffect(() => window.axo.onUpdateStatus(setUpdate), [])

  useEffect(() => {
    // First-run onboarding (P5-05): shown once, then persisted away.
    void window.axo.getSettings().then((s) => {
      if (!s.onboarded) {
        setOnboarding(s)
      }
    })
  }, [])

  useEffect(() => {
    // Silent session restore on startup (P2-07); falls back to Login.
    window.axo
      .restoreSession()
      .then(setSession)
      .catch(() => setSession(null))
      .finally(() => setRestoring(false))
    void window.axo.getVersion().then(setVersion)
  }, [])

  if (restoring) {
    return (
      <div className="login-screen">
        <div className="logo">
          <span className="logo-axo">AXO</span>
          <span className="logo-sub">CLIENT</span>
        </div>
      </div>
    )
  }

  if (!session) {
    return <LoginScreen onLoggedIn={setSession} />
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="logo">
          <img src={logoUrl} alt="" className="logo-mark" />
          <span className="logo-axo">AXO</span>
          <span className="logo-sub">CLIENT</span>
        </div>
        <nav>
          <button
            className={screen === 'home' ? 'nav-item active' : 'nav-item'}
            onClick={() => setScreen('home')}
          >
            Play
          </button>
          <button
            className={screen === 'settings' ? 'nav-item active' : 'nav-item'}
            onClick={() => setScreen('settings')}
          >
            Settings
          </button>
        </nav>
        <div className="sidebar-footer">
          <div className="account-chip" title={session.uuid}>
            <span className="account-dot" />
            {session.username}
          </div>
          <button
            className="link-button"
            onClick={() => {
              void window.axo.logout().then(() => setSession(null))
            }}
          >
            Sign out
          </button>
          <div className="app-version">v{version}</div>
        </div>
      </aside>
      {onboarding && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <img src={logoUrl} alt="" className="login-mark" />
            <h2>Welcome to Axo Client</h2>
            <p className="muted">
              How much memory should Minecraft get? You can change this later in Settings.
            </p>
            <p className="muted">{(onboarding.ramMb / 1024).toFixed(1)} GB</p>
            <input
              type="range"
              min={1024}
              max={16384}
              step={512}
              value={onboarding.ramMb}
              onChange={(e) => setOnboarding({ ...onboarding, ramMb: Number(e.target.value) })}
            />
            <p className="muted">
              Game files install to <code>{onboarding.installDir}</code>
            </p>
            <button
              className="primary-button"
              onClick={() => {
                void window.axo
                  .updateSettings({ ramMb: onboarding.ramMb, onboarded: true })
                  .then(() => setOnboarding(null))
              }}
            >
              Let&apos;s go
            </button>
          </div>
        </div>
      )}
      <main className="content">
        {update && (
          <div className="update-banner">
            Update {update.version} ready
            <button className="link-button" onClick={() => void window.axo.installUpdate()}>
              Restart to install
            </button>
          </div>
        )}
        {screen === 'home' ? <HomeScreen /> : <SettingsScreen />}
      </main>
    </div>
  )
}
