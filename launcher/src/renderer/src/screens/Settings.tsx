import { useEffect, useState } from 'react'
import type { AxoSettings } from '../../../shared/types'

const RAM_MIN = 1024
const RAM_MAX = 16384
const RAM_STEP = 512

export default function SettingsScreen(): React.JSX.Element {
  const [settings, setSettings] = useState<AxoSettings | null>(null)
  const [saving, setSaving] = useState(false)
  const [repairState, setRepairState] = useState<string | null>(null)

  const repair = (): void => {
    setRepairState('Repairing…')
    window.axo
      .repair()
      .then((r) =>
        setRepairState(`Repaired — ${r.downloaded} restored, ${r.kept} intact, ${r.removed} removed`)
      )
      .catch((e: unknown) =>
        setRepairState(e instanceof Error ? e.message : 'Repair failed — see logs.')
      )
  }

  useEffect(() => {
    void window.axo.getSettings().then(setSettings)
  }, [])

  const apply = (patch: Partial<AxoSettings>): void => {
    setSaving(true)
    window.axo
      .updateSettings(patch)
      .then(setSettings)
      .finally(() => setSaving(false))
  }

  if (!settings) {
    return (
      <div className="settings-screen">
        <h1>Settings</h1>
        <p className="muted">Loading…</p>
      </div>
    )
  }

  return (
    <div className="settings-screen">
      <h1>Settings {saving && <span className="saving-note">saving…</span>}</h1>

      <div className="settings-group">
        <h2>Game memory</h2>
        <p className="muted">
          {(settings.ramMb / 1024).toFixed(1)} GB allocated to Minecraft
        </p>
        <input
          type="range"
          min={RAM_MIN}
          max={RAM_MAX}
          step={RAM_STEP}
          value={settings.ramMb}
          onChange={(e) => setSettings({ ...settings, ramMb: Number(e.target.value) })}
          onMouseUp={() => apply({ ramMb: settings.ramMb })}
          onTouchEnd={() => apply({ ramMb: settings.ramMb })}
        />
      </div>

      <div className="settings-group">
        <h2>Advanced</h2>
        <label className="field-label">
          Extra JVM arguments
          <input
            type="text"
            value={settings.jvmArgs}
            placeholder="none"
            spellCheck={false}
            onChange={(e) => setSettings({ ...settings, jvmArgs: e.target.value })}
            onBlur={() => apply({ jvmArgs: settings.jvmArgs })}
          />
        </label>
        <p className="muted">
          Install location: <code>{settings.installDir}</code>
          <br />
          Moving the install directory arrives with roadmap task P5-06.
        </p>
      </div>

      <div className="settings-group">
        <h2>Maintenance</h2>
        <p className="muted">
          Repair re-checks every installed file against the version manifest and restores anything
          missing or corrupted.
        </p>
        <button className="link-button" onClick={repair} disabled={repairState === 'Repairing…'}>
          Repair installation
        </button>
        {repairState && <p className="muted">{repairState}</p>}
      </div>

      <div className="settings-group">
        <h2>About</h2>
        <p className="muted">
          Axo Launcher — updates are checked automatically in packaged builds.
        </p>
        <button className="link-button" onClick={() => void window.axo.openLogs()}>
          Open log folder
        </button>
      </div>
    </div>
  )
}
