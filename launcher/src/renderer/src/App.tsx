import { useEffect, useState } from 'react'
import HomeScreen from './screens/Home'
import LoginScreen from './screens/Login'
import SettingsScreen from './screens/Settings'
import type { SessionInfo } from '../../shared/types'

type Screen = 'home' | 'settings'

export default function App(): React.JSX.Element {
  const [screen, setScreen] = useState<Screen>('home')
  const [session, setSession] = useState<SessionInfo | null>(null)
  const [version, setVersion] = useState('')

  useEffect(() => {
    void window.axo.getSession().then(setSession)
    void window.axo.getVersion().then(setVersion)
  }, [])

  if (!session) {
    return <LoginScreen onLoggedIn={setSession} />
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="logo">
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
      <main className="content">
        {screen === 'home' ? <HomeScreen /> : <SettingsScreen />}
      </main>
    </div>
  )
}
