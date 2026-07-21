import { useCallback, useEffect, useState } from 'react'
import type { GameProgress, ManifestInfo, VersionStatus } from '../../../shared/types'
import { formatBytes, formatDuration, formatSpeed } from '../../../shared/format'

const STAGE_LABELS: Record<GameProgress['stage'], string> = {
  preparing: 'Preparing…',
  java: 'Setting up Java…',
  mods: 'Installing mods…',
  downloading: 'Downloading game…',
  launching: 'Launching…',
  running: 'Running',
  closed: 'Play'
}

const STATE_LABEL: Record<VersionStatus['state'], string> = {
  installed: 'Installed',
  partial: 'Needs repair',
  'not-installed': 'Not installed'
}

export default function HomeScreen({
  onGoToVersions,
  username
}: {
  onGoToVersions: () => void
  username: string
}): React.JSX.Element {
  const [manifest, setManifest] = useState<ManifestInfo | null>(null)
  const [manifestError, setManifestError] = useState<string | null>(null)
  const [channel, setChannel] = useState('stable')
  const [versionId, setVersionId] = useState<string | null>(null)
  const [progress, setProgress] = useState<GameProgress | null>(null)
  const [launchError, setLaunchError] = useState<string | null>(null)
  const [statuses, setStatuses] = useState<VersionStatus[] | null>(null)
  const [repairMsg, setRepairMsg] = useState<string | null>(null)
  const [repairing, setRepairing] = useState(false)

  const refreshStatuses = useCallback(() => {
    window.axo
      .listVersions()
      .then(setStatuses)
      .catch(() => undefined)
  }, [])

  useEffect(() => {
    window.axo
      .getManifest()
      .then((info) => {
        setManifest(info)
        const chan = info.channels[channel] ?? Object.values(info.channels)[0]
        if (chan) {
          setVersionId(chan.default)
        }
      })
      .catch((e: unknown) => {
        setManifestError(e instanceof Error ? e.message : 'Could not load version list.')
      })
    refreshStatuses()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(
    () =>
      window.axo.onGameProgress((p) => {
        setProgress(p)
        if (p.stage === 'closed') {
          refreshStatuses()
        }
        if (p.stage === 'closed' && p.detail?.startsWith('crash')) {
          setLaunchError(
            p.detail === 'crash-boot'
              ? 'Minecraft crashed while starting. Check the game logs (Open logs below) — if it keeps happening, use Repair.'
              : 'Minecraft crashed. Check the game logs via Open logs below.'
          )
        }
      }),
    [refreshStatuses]
  )

  const busy = progress !== null && progress.stage !== 'closed'
  const versions = manifest?.channels[channel]?.versions ?? []
  const selectedStatus = statuses?.find((s) => s.id === versionId) ?? null
  const needsInstall = selectedStatus === null || selectedStatus.state !== 'installed'

  const play = (): void => {
    if (!versionId) {
      return
    }
    setLaunchError(null)
    setProgress({ stage: 'preparing' })
    window.axo.launch(versionId).catch((e: unknown) => {
      setLaunchError(e instanceof Error ? e.message : 'Launch failed — see logs.')
      setProgress(null)
    })
  }

  const repair = (): void => {
    setRepairing(true)
    setRepairMsg('Repairing…')
    window.axo
      .repair()
      .then((r) =>
        setRepairMsg(`Repaired — ${r.downloaded} restored, ${r.kept} intact, ${r.removed} removed`)
      )
      .catch((e: unknown) => setRepairMsg(e instanceof Error ? e.message : 'Repair failed — see logs.'))
      .finally(() => {
        setRepairing(false)
        refreshStatuses()
      })
  }

  const playLabel = busy && progress ? STAGE_LABELS[progress.stage] : needsInstall ? 'Install & Play' : 'Play'

  return (
    <div className="home-screen">
      <header className="home-header">
        <div>
          <p className="home-greeting">Welcome back, {username}</p>
          <h1>Ready to play</h1>
        </div>
        {manifest?.stale && (
          <span className="stale-badge" title="Showing cached version list — network unavailable">
            offline data
          </span>
        )}
      </header>

      {manifestError && <div className="error-banner">{manifestError}</div>}
      {manifest?.forcedUpdate && (
        <div className="error-banner">
          This version of Axo Launcher is too old to play. An update will install automatically —
          or download the latest from the website.
        </div>
      )}
      {launchError && (
        <div className="error-banner">
          {launchError}
          <button className="link-button" onClick={play}>
            Retry
          </button>
        </div>
      )}

      <div className="play-panel">
        <div className="version-row">
          <label>
            Channel
            <select value={channel} disabled={busy} onChange={(e) => setChannel(e.target.value)}>
              {Object.keys(manifest?.channels ?? { stable: null }).map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Version
            <select
              value={versionId ?? ''}
              disabled={busy}
              onChange={(e) => setVersionId(e.target.value)}
            >
              {versions.map((v) => (
                <option key={v.id} value={v.id}>
                  Minecraft {v.mcVersion}
                </option>
              ))}
            </select>
          </label>
          {selectedStatus && (
            <span className={`state-badge state-${selectedStatus.state}`}>
              {STATE_LABEL[selectedStatus.state]}
            </span>
          )}
        </div>

        <button
          className="primary-button play-button"
          disabled={!versionId || busy || manifest?.forcedUpdate}
          onClick={play}
        >
          {playLabel}
        </button>

        {busy && progress && progress.received !== undefined && (
          <div className="progress">
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
              <span>
                {formatBytes(progress.received)}
                {progress.total ? ` / ${formatBytes(progress.total)}` : ''}
              </span>
              <span>
                {[formatSpeed(progress.bytesPerSecond ?? 0), formatDuration(progress.etaSeconds ?? null)]
                  .filter(Boolean)
                  .join(' · ')}
              </span>
            </div>
          </div>
        )}

        {progress?.stage === 'running' && (
          <button
            className="link-button"
            onClick={() => {
              if (window.confirm('Force close Minecraft? Unsaved progress will be lost.')) {
                void window.axo.forceClose()
              }
            }}
          >
            Force close game
          </button>
        )}

        <p className="muted">
          {busy && progress?.detail
            ? progress.detail
            : versions.find((v) => v.id === versionId)?.notes ?? 'Select a version to play.'}
        </p>
      </div>

      <div className="quick-actions">
        <button className="btn-secondary" onClick={repair} disabled={busy || repairing}>
          {repairing ? 'Repairing…' : 'Repair install'}
        </button>
        <button className="btn-secondary" onClick={() => void window.axo.openLogs()}>
          Open logs
        </button>
        <button className="btn-secondary" onClick={onGoToVersions} disabled={busy}>
          Manage versions
        </button>
      </div>
      {repairMsg && <p className="muted repair-msg">{repairMsg}</p>}
    </div>
  )
}
