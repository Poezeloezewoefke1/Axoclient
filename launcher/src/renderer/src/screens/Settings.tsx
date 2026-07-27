import { useEffect, useState } from 'react'
import type { AxoSettings } from '../../../shared/types'

const RAM_MIN = 1024
const RAM_MAX = 16384
const RAM_STEP = 512

interface JvmPreset {
  id: string
  label: string
  description: string
  args: string
}

/** "3h 20m", or "not yet" before the first session. */
function formatPlaytime(minutes: number): string {
  if (minutes <= 0) {
    return 'No games played yet'
  }
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) {
    return `${rest} minutes played`
  }
  return `${hours}h ${rest}m played`
}

export default function SettingsScreen(): React.JSX.Element {
  const [settings, setSettings] = useState<AxoSettings | null>(null)
  const [saving, setSaving] = useState(false)
  const [repairState, setRepairState] = useState<string | null>(null)
  const [recommendedRam, setRecommendedRam] = useState<number | null>(null)
  const [presets, setPresets] = useState<JvmPreset[]>([])
  const [log, setLog] = useState<string | null>(null)

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
    void window.axo.getRecommendedRam().then(setRecommendedRam).catch(() => undefined)
    void window.axo.getJvmPresets().then(setPresets).catch(() => undefined)
  }, [])

  const apply = (patch: Partial<AxoSettings>): void => {
    setSaving(true)
    window.axo
      .updateSettings(patch)
      .then(setSettings)
      .finally(() => setSaving(false))
  }

  const toggleLog = (): void => {
    if (log !== null) {
      setLog(null)
      return
    }
    void window.axo
      .readLog()
      .then((text) => setLog(text || 'The log is empty.'))
      .catch(() => setLog('Could not read the log.'))
  }

  if (!settings) {
    return (
      <div className="settings-screen">
        <h1>Settings</h1>
        <p className="muted">Loading…</p>
      </div>
    )
  }

  const activePreset =
    presets.find((p) => p.args === settings.jvmArgs.trim())?.id ?? 'custom'

  return (
    <div className="settings-screen">
      <h1>Settings {saving && <span className="saving-note">saving…</span>}</h1>

      <div className="settings-group">
        <h2>Game memory</h2>
        <p className="muted">{(settings.ramMb / 1024).toFixed(1)} GB allocated to Minecraft</p>
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
        {recommendedRam !== null && (
          <p className="muted">
            Recommended for your PC: {(recommendedRam / 1024).toFixed(1)} GB
            {settings.ramMb !== recommendedRam && (
              <button
                className="link-button"
                style={{ marginLeft: 10 }}
                onClick={() => apply({ ramMb: recommendedRam })}
              >
                Use this
              </button>
            )}
          </p>
        )}
      </div>

      <div className="settings-group">
        <h2>Speed preset</h2>
        <p className="muted">
          Changes how Java runs the game. Balanced is safest; try Performance for smoother frames.
        </p>
        <div className="preset-list">
          {presets.map((preset) => (
            <button
              key={preset.id}
              className={activePreset === preset.id ? 'preset active' : 'preset'}
              onClick={() => apply({ jvmArgs: preset.args })}
            >
              <span className="preset-label">{preset.label}</span>
              <span className="preset-desc">{preset.description}</span>
            </button>
          ))}
        </div>
        {activePreset === 'custom' && (
          <p className="muted">Using your own custom settings (below).</p>
        )}
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
        <h2>Logs</h2>
        <div className="quick-actions" style={{ justifyContent: 'flex-start' }}>
          <button className="chip-action" onClick={toggleLog}>
            {log !== null ? 'Hide log' : 'View log'}
          </button>
          <button className="chip-action" onClick={() => void window.axo.openLogs()}>
            Open log folder
          </button>
        </div>
        {log !== null && <pre className="log-view">{log}</pre>}
      </div>

      <div className="settings-group">
        <h2>About</h2>
        <p className="muted">{formatPlaytime(settings.playtimeMinutes)}</p>
        <p className="muted">Axo Launcher — updates are checked automatically in packaged builds.</p>
      </div>
    </div>
  )
}
