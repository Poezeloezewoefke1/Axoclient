import { useCallback, useEffect, useState } from 'react'
import type { GameProgress, VersionStatus } from '../../../shared/types'
import { formatBytes, formatDuration, formatSpeed } from '../../../shared/format'

const STATE_LABEL: Record<VersionStatus['state'], string> = {
  installed: 'Installed',
  partial: 'Needs repair',
  'not-installed': 'Not installed'
}

export default function VersionsScreen(): React.JSX.Element {
  const [versions, setVersions] = useState<VersionStatus[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [progress, setProgress] = useState<GameProgress | null>(null)

  const refresh = useCallback(() => {
    window.axo
      .listVersions()
      .then(setVersions)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Could not load versions.'))
  }, [])

  useEffect(() => refresh(), [refresh])
  useEffect(() => window.axo.onGameProgress(setProgress), [])

  const install = (id: string): void => {
    setBusyId(id)
    setError(null)
    window.axo
      .installVersion(id)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Install failed — see logs.'))
      .finally(() => {
        setBusyId(null)
        setProgress(null)
        refresh()
      })
  }

  const remove = (id: string): void => {
    if (!window.confirm(`Delete ${id}? This removes its mods and saves.`)) {
      return
    }
    setBusyId(id)
    window.axo
      .deleteVersion(id)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : 'Delete failed.'))
      .finally(() => {
        setBusyId(null)
        refresh()
      })
  }

  return (
    <div className="versions-screen">
      <div className="versions-head">
        <h1>Versions</h1>
        <button className="link-button" onClick={refresh} disabled={busyId !== null}>
          Refresh
        </button>
      </div>
      <p className="muted">Install, update, verify, or remove each Axo version independently.</p>

      {error && <div className="error-banner">{error}</div>}

      {!versions && <p className="muted">Loading…</p>}

      <div className="version-list">
        {versions?.map((v) => {
          const busy = busyId === v.id
          const installed = v.state === 'installed'
          return (
            <div className="version-row-card" key={v.id}>
              <div className="version-info">
                <div className="version-title">
                  Minecraft {v.mcVersion}
                  <span className={`state-badge state-${v.state}`}>{STATE_LABEL[v.state]}</span>
                  <span className="tag">{v.channel}</span>
                </div>
                <div className="muted version-sub">
                  {v.id} · {v.presentFiles}/{v.expectedFiles} files
                  {v.corrupted.length > 0 ? ` · ${v.corrupted.length} corrupted` : ''}
                  {v.missing.length > 0 ? ` · ${v.missing.length} missing` : ''}
                </div>
                {busy && progress && progress.received !== undefined && (
                  <div className="progress" style={{ marginTop: 10 }}>
                    <div className="progress-bar">
                      <div
                        className="progress-fill"
                        style={{
                          width:
                            progress.total && progress.total > 0
                              ? `${Math.min(100, (progress.received / progress.total) * 100)}%`
                              : '100%'
                        }}
                      />
                    </div>
                    <div className="progress-stats">
                      <span>{progress.detail}</span>
                      <span>
                        {[
                          formatSpeed(progress.bytesPerSecond ?? 0),
                          formatDuration(progress.etaSeconds ?? null)
                        ]
                          .filter(Boolean)
                          .join(' · ')}
                      </span>
                    </div>
                  </div>
                )}
              </div>
              <div className="version-actions">
                <button className="btn-secondary" disabled={busy} onClick={() => install(v.id)}>
                  {busy ? 'Working…' : installed ? 'Update / Repair' : 'Install'}
                </button>
                {installed && (
                  <button className="link-button danger" disabled={busy} onClick={() => remove(v.id)}>
                    Delete
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>

      <div className="note-box">
        Each version installs into its own instance folder, so mods and saves never mix. Deleting a
        version frees its disk space but keeps the shared game files for others.
      </div>
    </div>
  )
}
