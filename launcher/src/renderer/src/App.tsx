import { useCallback, useEffect, useState } from 'react'
import logoUrl from './assets/logo.png'
import SkinRender from './components/SkinRender'
import { IconCaret, IconLogout, IconPlay, IconSettings, IconSkin, IconVersions } from './components/Icons'
import HomeScreen from './screens/Home'
import LoginScreen from './screens/Login'
import SettingsScreen from './screens/Settings'
import SkinsScreen from './screens/Skins'
import VersionsScreen from './screens/Versions'
import type { AccountInfo, AxoSettings, SessionInfo, SkinInfo, UpdateStatus } from '../../shared/types'

type Screen = 'play' | 'skins' | 'versions' | 'settings'

const SCREEN_TITLE: Record<Screen, string> = {
  play: 'Play',
  skins: 'Skins',
  versions: 'Versions',
  settings: 'Settings'
}

export default function App(): React.JSX.Element {
  const [screen, setScreen] = useState<Screen>('play')
  const [session, setSession] = useState<SessionInfo | null>(null)
  const [restoring, setRestoring] = useState(true)
  const [version, setVersion] = useState('')
  const [update, setUpdate] = useState<UpdateStatus | null>(null)
  const [onboarding, setOnboarding] = useState<AxoSettings | null>(null)
  const [skin, setSkin] = useState<SkinInfo | null>(null)
  const [menuOpen, setMenuOpen] = useState(false)
  const [accounts, setAccounts] = useState<AccountInfo[]>([])
  const [accountBusy, setAccountBusy] = useState(false)

  const refreshSkin = useCallback(() => {
    void window.axo
      .getSkin()
      .then(setSkin)
      .catch(() => setSkin(null))
  }, [])

  const loadAccounts = useCallback(() => {
    void window.axo
      .listAccounts()
      .then(setAccounts)
      .catch(() => setAccounts([]))
  }, [])

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

  useEffect(() => {
    if (session) {
      refreshSkin()
      loadAccounts()
    } else {
      setSkin(null)
      setAccounts([])
    }
  }, [session, refreshSkin, loadAccounts])

  const switchAccount = (uuid: string): void => {
    setAccountBusy(true)
    window.axo
      .selectAccount(uuid)
      .then((s) => {
        if (s) {
          setSession(s)
        }
      })
      .catch(() => undefined)
      .finally(() => {
        setAccountBusy(false)
        setMenuOpen(false)
      })
  }

  const addAccount = (): void => {
    setAccountBusy(true)
    window.axo
      .login()
      .then((s) => setSession(s))
      .catch(() => undefined)
      .finally(() => {
        setAccountBusy(false)
        setMenuOpen(false)
      })
  }

  const removeAccount = (uuid: string): void => {
    setAccountBusy(true)
    window.axo
      .removeAccount(uuid)
      .then((s) => setSession(s))
      .catch(() => undefined)
      .finally(() => {
        setAccountBusy(false)
        loadAccounts()
      })
  }

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

  const railItem = (id: Screen, icon: React.JSX.Element, label: string): React.JSX.Element => (
    <button
      className={screen === id ? 'rail-item active' : 'rail-item'}
      onClick={() => setScreen(id)}
      title={label}
    >
      {icon}
      <span>{label}</span>
    </button>
  )

  return (
    <div className="shell">
      <aside className="rail">
        <div className="rail-logo">
          <img src={logoUrl} alt="" />
        </div>
        <nav className="rail-nav">
          {railItem('play', <IconPlay />, 'Play')}
          {railItem('skins', <IconSkin />, 'Skins')}
          {railItem('versions', <IconVersions />, 'Versions')}
          {railItem('settings', <IconSettings />, 'Settings')}
        </nav>
        <div className="rail-foot">v{version}</div>
      </aside>

      <div className="stage">
        <header className="topbar">
          <div className="topbar-title">{SCREEN_TITLE[screen]}</div>
          <div className="topbar-right">
            <div className="account">
              <button className="account-btn" onClick={() => setMenuOpen((v) => !v)}>
                <span className="account-avatar">
                  {skin?.dataUrl ? (
                    <SkinRender dataUrl={skin.dataUrl} slim={skin.slim} scale={4} view="head" />
                  ) : (
                    <span className="account-avatar-fallback">{session.username[0]?.toUpperCase()}</span>
                  )}
                </span>
                <span className="account-name">{session.username}</span>
                <IconCaret size={16} />
              </button>
              {menuOpen && (
                <>
                  <div className="menu-scrim" onClick={() => setMenuOpen(false)} />
                  <div className="account-menu">
                    <div className="account-menu-head">
                      <div className="account-menu-name">Accounts</div>
                      <div className="muted account-menu-sub">Switch or add a Microsoft account</div>
                    </div>
                    <div className="account-list">
                      {accounts.map((a) => (
                        <div
                          key={a.uuid}
                          className={a.active ? 'account-row active' : 'account-row'}
                        >
                          <button
                            className="account-row-main"
                            disabled={accountBusy || a.active}
                            onClick={() => switchAccount(a.uuid)}
                          >
                            <span className="account-row-dot" />
                            <span className="account-row-name">{a.username}</span>
                            {a.active && <span className="account-badge">Playing</span>}
                          </button>
                          <button
                            className="account-row-remove"
                            disabled={accountBusy}
                            title="Remove account"
                            onClick={() => removeAccount(a.uuid)}
                          >
                            ×
                          </button>
                        </div>
                      ))}
                    </div>
                    <button
                      className="account-menu-item"
                      disabled={accountBusy}
                      onClick={addAccount}
                    >
                      <span className="account-add-plus">+</span>
                      {accountBusy ? 'Working…' : 'Add account'}
                    </button>
                    <button
                      className="account-menu-item danger"
                      disabled={accountBusy}
                      onClick={() => {
                        setMenuOpen(false)
                        void window.axo.logout().then((s) => setSession(s))
                      }}
                    >
                      <IconLogout size={16} />
                      Sign out
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </header>

        <main className="content">
          {update && (
            <div className="update-banner">
              Update {update.version} ready
              <button className="link-button" onClick={() => void window.axo.installUpdate()}>
                Restart to install
              </button>
            </div>
          )}
          {screen === 'play' && (
            <HomeScreen
              onGoToVersions={() => setScreen('versions')}
              username={session.username}
              skin={skin}
            />
          )}
          {screen === 'skins' && <SkinsScreen skin={skin} onChanged={refreshSkin} />}
          {screen === 'versions' && <VersionsScreen />}
          {screen === 'settings' && <SettingsScreen />}
        </main>
      </div>

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
    </div>
  )
}
